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
import com.ruskserver.deepwither_V2.modules.item.durability.EquipmentDurabilityService;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
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

    private static final double DEFAULT_ATTACKS_PER_SECOND = 1.0;
    private static final double MIN_ATTACKS_PER_SECOND = 0.2;
    private static final double MAX_ATTACKS_PER_SECOND = 3.0;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;
    private final StatManager statManager;
    private final ManaManager manaManager;
    private final DamagePipelineManager damagePipelineManager;
    private final Deepwither_V2 plugin;
    private final EquipmentDurabilityService durabilityService;

    @Inject
    public WandAttackListener(ItemManager itemManager, ItemPDCUtil pdcUtil, StatManager statManager,
                              ManaManager manaManager, DamagePipelineManager damagePipelineManager,
                              Deepwither_V2 plugin, EquipmentDurabilityService durabilityService) {
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
        this.statManager = statManager;
        this.manaManager = manaManager;
        this.damagePipelineManager = damagePipelineManager;
        this.plugin = plugin;
        this.durabilityService = durabilityService;
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
        if (!durabilityService.canUse(player, itemStack, true)) {
            event.setCancelled(true);
            return;
        }

        // ブロック破壊イベントなどをキャンセル
        event.setCancelled(true);

        // --- クールダウン処理 ---
        // 攻撃速度(ATTACK_SPEED)からクールダウンを計算。例: 1.5 なら 1秒間に1.5回発射可能 = 1000 / 1.5 = 666ms
        double attackSpeed = statManager.getTotalStat(player, StatType.ATTACK_SPEED);
        if (!wand.getBaseStats().containsKey(StatType.ATTACK_SPEED)) {
            attackSpeed += DEFAULT_ATTACKS_PER_SECOND;
        }
        attackSpeed = Math.max(MIN_ATTACKS_PER_SECOND, Math.min(MAX_ATTACKS_PER_SECOND, attackSpeed));

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
        durabilityService.damageItem(player, itemStack, 1);

        // --- 魔法弾の発射 ---
        shootMagicMissile(player, wand);
    }

    private void shootMagicMissile(Player shooter, WandItem wand) {
        // 発射音
        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);

        // 弾の始点（目の位置から少し下）と進行方向
        Location loc = shooter.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = loc.getDirection().normalize();
        var world = loc.getWorld();

        // 発射エフェクト：杖先のリング
        TrailCircleHelper.spawnCircle(loc, 0.4, Color.fromRGB(135, 206, 235), 6, 8, direction, 45);
        world.spawnParticle(Particle.GLOW, loc, 5, 0.1, 0.1, 0.1, 0.02);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 0, direction.getX(), direction.getY(), direction.getZ(), 0.15);

        final double speed = wand.getProjectileSpeed();
        final double range = wand.getMaxRange();
        final Particle particleType = wand.getProjectileParticle();
        boolean isColored = particleType.getDataType() == Color.class;

        new BukkitRunnable() {
            double distanceTraveled = 0;

            @Override
            public void run() {
                loc.add(direction.clone().multiply(speed));
                distanceTraveled += speed;

                if (distanceTraveled > range || loc.getBlock().getType().isSolid()) {
                    impactEffect(loc, world, particleType, isColored);
                    this.cancel();
                    return;
                }

                // 弾本体のパーティクル
                if (isColored) {
                    world.spawnParticle(particleType, loc, 1, 0, 0, 0, 0, Color.WHITE);
                } else {
                    world.spawnParticle(particleType, loc, 1, 0, 0, 0, 0);
                }
                world.spawnParticle(Particle.GLOW, loc, 1, 0.05, 0.05, 0.05, 0);

                // ベクトルパーティクルの軌跡
                world.spawnParticle(particleType, loc, 0, direction.getX(), direction.getY(), direction.getZ(), 0.08);

                // TRAILリング
                TrailCircleHelper.spawnCircle(loc, 0.2, Color.fromRGB(100, 149, 237), 3, 4, direction, distanceTraveled * 30);

                // 当たり判定 (半径0.8ブロックの球体)
                for (Entity target : world.getNearbyEntities(loc, 0.8, 0.8, 0.8)) {
                    if (target instanceof LivingEntity livingTarget && target != shooter) {
                        damagePipelineManager.processDamage(shooter, livingTarget, DamageType.MAGIC,
                                0.0, wand.getTags(), "basic_wand", 0L, loc);

                        livingTarget.getWorld().playSound(livingTarget.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
                        hitEffect(livingTarget.getLocation().add(0, 1, 0), world, direction);

                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void hitEffect(Location loc, World world, Vector dir) {
        world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0, Color.WHITE);
        world.spawnParticle(Particle.GLOW, loc, 10, 0.5, 0.5, 0.5, 0.05);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.2);
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            double x = Math.cos(angle) * 0.5;
            double z = Math.sin(angle) * 0.5;
            world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 0, x, 0.2, z, 0.1);
        }
        TrailCircleHelper.spawnCircle(loc, 1.0, Color.fromRGB(100, 149, 237), 6, 12);
        TrailCircleHelper.spawnCircle(loc, 0.7, Color.fromRGB(70, 130, 255), 5, 10, new Vector(0, 1, 0), 30);
    }

    private void impactEffect(Location loc, World world, Particle particleType, boolean isColored) {
        world.spawnParticle(Particle.POOF, loc, 8, 0.1, 0.1, 0.1, 0.05);
        world.spawnParticle(Particle.GLOW, loc, 5, 0.3, 0.3, 0.3, 0.02);
        TrailCircleHelper.spawnCircle(loc, 0.5, Color.fromRGB(150, 150, 150), 4, 6);
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
