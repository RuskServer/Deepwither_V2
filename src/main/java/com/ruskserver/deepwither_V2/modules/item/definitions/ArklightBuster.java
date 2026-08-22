package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.party.PartyManager;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailHelper;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ArklightBuster implements CustomItem {

    private static final int MAX_CHARGES = 5;
    private static final long CHARGE_EXPIRE_MILLIS = 10_000L; // 10秒

    private final Map<StatType, Double> baseStats;
    private final JavaPlugin plugin;
    private final DamagePipelineManager damageManager;
    private final PartyManager partyManager;

    private record ChargeState(int charges, long expireTime) {}
    private final Map<UUID, ChargeState> playerCharges = new ConcurrentHashMap<>();

    @Inject
    public ArklightBuster(JavaPlugin plugin, DamagePipelineManager damageManager, PartyManager partyManager) {
        this.plugin = plugin;
        this.damageManager = damageManager;
        this.partyManager = partyManager;

        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 48.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.8);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 12.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 6.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 160.0);
    }

    @Override
    public String getId() {
        return "arklight_buster";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§fAWS-GS07「アークライト・バスター」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public String getFlavorText() {
        return "ArkWorks Systemsが開発した対大型兵装大剣。通常攻撃でアークチャージを蓄積し、最大チャージ（5スタック）時の攻撃で周囲5mの空間に無数の斜め光刃を放ち一閃する。";
    }

    @Override
    public int getCustomModelData() {
        return 25;
    }

    @Override
    public double getSellPrice() {
        return 1500.0;
    }

    @Override
    public String getWeaponType() {
        return "大剣";
    }

    @Override
    public void onAttack(DamageContext context) {
        if (!(context.getAttacker() instanceof Player player)) return;
        if (!(context.getDefender() instanceof LivingEntity victim) || victim instanceof ArmorStand) return;
        if (context.hasTag("ARKLIGHT_BURST")) return; // 再帰発火防止

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        ChargeState state = playerCharges.getOrDefault(playerId, new ChargeState(0, 0L));

        int currentCharges = (now <= state.expireTime()) ? state.charges() : 0;

        if (currentCharges >= MAX_CHARGES) {
            // フルチャージ解放
            playerCharges.remove(playerId);
            executeArkBurst(player, victim.getLocation());
        } else {
            // チャージ蓄積
            int nextCharges = currentCharges + 1;
            playerCharges.put(playerId, new ChargeState(nextCharges, now + CHARGE_EXPIRE_MILLIS));
            showChargeFeedback(player, nextCharges);
        }
    }

    private void showChargeFeedback(Player player, int charges) {
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= MAX_CHARGES; i++) {
            if (i <= charges) {
                bar.append("▮");
            } else {
                bar.append("▯");
            }
        }

        if (charges >= MAX_CHARGES) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("✦ アークチャージ完了！ [")
                    .color(NamedTextColor.AQUA)
                    .append(net.kyori.adventure.text.Component.text(bar.toString(), NamedTextColor.YELLOW))
                    .append(net.kyori.adventure.text.Component.text("] (次撃で全方位解放)", NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false));
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.6f);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1.0, 0), 12, 0.4, 0.4, 0.4, 0.05);
        } else {
            player.sendActionBar(net.kyori.adventure.text.Component.text("✦ アークチャージ: [")
                    .color(NamedTextColor.YELLOW)
                    .append(net.kyori.adventure.text.Component.text(bar.toString(), NamedTextColor.GOLD))
                    .append(net.kyori.adventure.text.Component.text("] (" + charges + "/" + MAX_CHARGES + ")", NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 1.2f + (charges * 0.15f));
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1.0, 0), 4, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void executeArkBurst(Player player, Location center) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        player.sendActionBar(net.kyori.adventure.text.Component.text("✦ アーク・バースト解放！ ✦")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        // 重厚な効果音
        world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        world.playSound(playerLoc, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.4f);
        world.playSound(playerLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.8f);

        // 衝撃波リング（半径5m）
        TrailCircleHelper.spawnCircle(playerLoc.clone().add(0, 0.2, 0), 5.0, Color.fromRGB(120, 220, 255), 16, 32);

        // 周囲5ブロック圏内にランダムな斜め斬撃を26本展開
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Color lightCyan = Color.fromRGB(160, 240, 255);
        Color arkBlue = Color.fromRGB(80, 160, 255);
        Color arkWhite = Color.fromRGB(240, 250, 255);

        for (int i = 0; i < 26; i++) {
            double angle = random.nextDouble(0, Math.PI * 2);
            double dist = random.nextDouble(0.5, 4.8);
            double cx = playerLoc.getX() + Math.cos(angle) * dist;
            double cy = playerLoc.getY() + random.nextDouble(0.2, 2.5);
            double cz = playerLoc.getZ() + Math.sin(angle) * dist;
            Location slashCenter = new Location(world, cx, cy, cz);

            double slashLength = random.nextDouble(1.5, 2.8);
            double yawAngle = random.nextDouble(0, Math.PI * 2);
            double pitchAngle = random.nextDouble(-Math.PI / 3, Math.PI / 3);

            Vector dir = new Vector(
                    Math.cos(yawAngle) * Math.cos(pitchAngle),
                    Math.sin(pitchAngle),
                    Math.sin(yawAngle) * Math.cos(pitchAngle)
            ).normalize().multiply(slashLength * 0.5);

            Location from = slashCenter.clone().subtract(dir);
            Location to = slashCenter.clone().add(dir);

            Color slashColor = (i % 3 == 0) ? arkWhite : (i % 2 == 0 ? lightCyan : arkBlue);
            TrailHelper.spawnLine(from, to, slashColor, 12);

            world.spawnParticle(Particle.SWEEP_ATTACK, slashCenter, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.ELECTRIC_SPARK, slashCenter, 3, 0.2, 0.2, 0.2, 0.05);
        }

        // 周囲5ブロックの敵に範囲ダメージ（攻撃力 × 180%）
        double radius = 5.0;
        List<LivingEntity> enemies = world.getNearbyLivingEntities(playerLoc, radius, radius, radius).stream()
                .filter(e -> !e.equals(player))
                .filter(e -> !(e instanceof ArmorStand))
                .filter(e -> !(e instanceof Player p && isSameParty(player, p)))
                .toList();

        for (LivingEntity enemy : enemies) {
            damageManager.processScaledDamage(
                    player,
                    enemy,
                    DamageType.PHYSICAL,
                    1.8,
                    Set.of("ARKLIGHT_BURST", "SLASH", "LIGHTNING", "AOE"),
                    "item:arklight_buster:burst",
                    300L,
                    enemy.getLocation()
            );
        }
    }

    private boolean isSameParty(Player p1, Player p2) {
        var party1 = partyManager.getParty(p1);
        return party1 != null && party1.isMember(p2.getUniqueId());
    }
}
