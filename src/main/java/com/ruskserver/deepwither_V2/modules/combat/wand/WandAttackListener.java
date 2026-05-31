package com.ruskserver.deepwither_V2.modules.combat.wand;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.WandItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WandItem インターフェースを実装したアイテムの共通左クリック攻撃（魔法弾）を処理するリスナー。
 */
@Component
public class WandAttackListener implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;
    private final StatManager statManager;
    private final ManaManager manaManager;
    private final DamagePipelineManager damagePipelineManager;
    private final Deepwither_V2 plugin;

    @Inject
    public WandAttackListener(ItemManager itemManager, ItemPDCUtil pdcUtil, StatManager statManager,
                              ManaManager manaManager, DamagePipelineManager damagePipelineManager,
                              Deepwither_V2 plugin) {
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
        this.statManager = statManager;
        this.manaManager = manaManager;
        this.damagePipelineManager = damagePipelineManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // 左クリック（空気中 または ブロック）のみ反応
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (itemStack == null || itemStack.isEmpty()) return;

        // アイテムのPDCからIDを取得
        String customId = pdcUtil.getItemId(itemStack);
        if (customId == null) return;

        // アイテム定義を取得し、それが魔法の杖(WandItem)であるか判定
        CustomItem customItem = itemManager.getCustomItem(customId);
        if (!(customItem instanceof WandItem wand)) return;

        // ブロック破壊イベントなどをキャンセル
        event.setCancelled(true);

        // --- クールダウン処理 ---
        // 攻撃速度(ATTACK_SPEED)からクールダウンを計算。例: 1.5 なら 1秒間に1.5回発射可能 = 1000 / 1.5 = 666ms
        double attackSpeed = statManager.getTotalStat(player, StatType.ATTACK_SPEED);
        if (attackSpeed <= 0) attackSpeed = 1.0; // デフォルト 1.0回/秒

        long cooldownMs = (long) (1000.0 / attackSpeed);
        long lastUse = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();

        if (now - lastUse < cooldownMs) {
            return; // クールダウン中
        }

        // --- マナ消費処理 ---
        double cost = wand.getManaCost();
        if (cost > 0) {
            if (!manaManager.consume(player, cost)) {
                player.sendMessage(net.kyori.adventure.text.Component.text("マナが足りません！", NamedTextColor.RED));
                return; // マナ不足
            }
        }

        // クールダウン更新
        cooldowns.put(player.getUniqueId(), now);

        // --- 魔法弾の発射 ---
        shootMagicMissile(player, wand);
    }

    private void shootMagicMissile(Player shooter, WandItem wand) {
        // 発射音
        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);

        // 弾の始点（目の位置から少し下）と進行方向
        Location loc = shooter.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = loc.getDirection().normalize();

        // 魔法攻撃力を取得（基礎値。パイプラインのBaseDamagePhaseで最終計算されますが、初期ダメージとして0を渡せばBaseDamagePhaseで自動取得されます）
        final double speed = wand.getProjectileSpeed();
        final double range = wand.getMaxRange();
        final Particle particleType = wand.getProjectileParticle();

        new BukkitRunnable() {
            double distanceTraveled = 0;

            @Override
            public void run() {
                // 移動
                loc.add(direction.clone().multiply(speed));
                distanceTraveled += speed;

                // 射程限界またはブロック（固体）衝突判定
                if (distanceTraveled > range || loc.getBlock().getType().isSolid()) {
                    // 壁に当たったエフェクト
                    loc.getWorld().spawnParticle(Particle.POOF, loc, 5, 0.1, 0.1, 0.1, 0.05);
                    this.cancel();
                    return;
                }

                // 弾の軌跡エフェクト
                spawnParticle(loc.getWorld(), particleType, loc, 1, 0, 0, 0, 0, Color.WHITE);

                // 当たり判定 (半径0.8ブロックの球体)
                for (Entity target : loc.getWorld().getNearbyEntities(loc, 0.8, 0.8, 0.8)) {
                    if (target instanceof LivingEntity livingTarget && target != shooter) {

                        // ダメージパイプラインに魔法ダメージとして処理を委譲
                        // (initialDamageは0で渡すことで、パイプラインのBaseDamagePhaseが攻撃者のMAGIC_DAMAGEを自動参照してくれます)
                        damagePipelineManager.processDamage(shooter, livingTarget, DamageType.MAGIC, 0.0, wand.getTags());

                        // ヒット演出
                        livingTarget.getWorld().playSound(livingTarget.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
                        spawnParticle(livingTarget.getWorld(), Particle.FLASH, livingTarget.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0, Color.WHITE);

                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnParticle(World world, Particle particle, Location location, int count,
                               double offsetX, double offsetY, double offsetZ, double extra, Color color) {
        if (particle.getDataType() == Color.class) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, color);
            return;
        }
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }
}
