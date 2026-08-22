package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.party.PartyManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

/**
 * 近衛スケルトン (Vanguard Skeleton)
 * <p>
 * 盾と大剣『アークライト・バスター』を巧みに操り、シールドバッシュ・なぎ払い・突進ダッシュ・リポスト（反撃）を繰り出す精鋭モブ。
 */
@Component
public class VanguardSkeletonMob extends CustomMob {

    public static final String MOB_ID = "vanguard_skeleton";
    private static final String WEAPON_ID = "arklight_buster";
    private static final String CHEST_ID = "laps_jacket";
    private static final String BOOTS_ID = "laps_tread";

    private static final double MAX_HP = 120.0;
    private static final double BASE_ATTACK_DAMAGE = 20.0;
    private static final double BASE_DEFENSE = 50.0;
    private static final double BASE_MAGIC_DEFENSE = 30.0;
    private static final int EXP_REWARD = 160;

    private final DamagePipelineManager damageManager;
    private final ItemManager itemManager;
    private final PartyManager partyManager;

    private int attackCount = 0;
    private long lastDashTime = 0L;

    @Inject
    public VanguardSkeletonMob(CustomMobManager mobManager,
                               DamagePipelineManager damageManager,
                               ItemManager itemManager,
                               PartyManager partyManager) {
        mobManager.registerMob(MOB_ID, EntityType.SKELETON,
                () -> new VanguardSkeletonMob(mobManager, damageManager, itemManager, partyManager));
        mobManager.registerDisplayName(MOB_ID, "近衛スケルトン");
        this.damageManager = damageManager;
        this.itemManager = itemManager;
        this.partyManager = partyManager;
    }

    @Override
    public void onSpawn() {
        setMaxHealth(MAX_HP);
        setExp(EXP_REWARD);
        setBaseName("近衛スケルトン");

        entity.customName(net.kyori.adventure.text.Component.text("近衛スケルトン", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        entity.setCustomNameVisible(true);

        if (entity instanceof Skeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
        }

        var attackAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttr != null) attackAttr.setBaseValue(BASE_ATTACK_DAMAGE);

        var speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.22);

        var kbAttr = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(0.5);

        // 装備の適用
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            ItemStack weapon = itemManager.generate(WEAPON_ID);
            if (weapon != null) equipment.setItemInMainHand(weapon);

            equipment.setItemInOffHand(new ItemStack(Material.SHIELD));

            ItemStack chest = itemManager.generate(CHEST_ID);
            if (chest != null) equipment.setChestplate(chest);

            ItemStack boots = itemManager.generate(BOOTS_ID);
            if (boots != null) equipment.setBoots(boots);

            equipment.setDropChance(EquipmentSlot.HAND, 0.0f);
            equipment.setDropChance(EquipmentSlot.OFF_HAND, 0.0f);
            equipment.setDropChance(EquipmentSlot.CHEST, 0.0f);
            equipment.setDropChance(EquipmentSlot.FEET, 0.0f);
        }
    }

    @Override
    public void onTick() {
        // 25tick (1.25秒) ごとにAIスキル判定
        if (ticksLived % 25 == 0) {
            LivingEntity target = getTarget();
            if (target != null) {
                double dist = entity.getLocation().distance(target.getLocation());
                if (dist < 3.5) {
                    // 近距離: 50%でシールドバッシュ、50%でなぎ払い
                    if (RANDOM.nextBoolean()) {
                        performShieldBash(target);
                    } else {
                        performCircularSwing();
                    }
                } else if (dist >= 6.0 && dist <= 12.0) {
                    long now = System.currentTimeMillis();
                    if (now - lastDashTime >= 4000L) { // 4秒クールダウン
                        lastDashTime = now;
                        performTacticalDash(target);
                    }
                }
            }
        }
    }

    @Override
    public void onAttack(LivingEntity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player player)) return;

        attackCount++;
        double damage = BASE_ATTACK_DAMAGE;

        // 3撃目ごとの強力な一撃
        if (attackCount % 3 == 0) {
            damage *= 1.5;
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.5);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.8f);
            player.sendMessage(net.kyori.adventure.text.Component.text("§c>>> 近衛スケルトンの強撃！"));
        }

        damageManager.processDamage(entity, player, DamageType.PHYSICAL, damage, Set.of("SLASH"));
    }

    @Override
    public void onDamaged(LivingEntity attacker, EntityDamageByEntityEvent event) {
        // 45%の確率で盾ガード
        if (RANDOM.nextDouble() < 0.45) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f);
            entity.getWorld().spawnParticle(
                    Particle.BLOCK,
                    entity.getLocation().add(0, 1.0, 0),
                    10,
                    0.3, 0.3, 0.3,
                    Bukkit.createBlockData(Material.IRON_BLOCK)
            );

            // リポスト (反撃)
            if (attacker != null && entity.getLocation().distance(attacker.getLocation()) <= 4.0) {
                performRiposte(attacker);
            }
        }
    }

    @Override
    public void onDeath() {
        Location deathLoc = getLocation();
        deathLoc.getWorld().spawnParticle(Particle.SMOKE, deathLoc.clone().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.03);
        deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_SKELETON_DEATH, 1.0f, 0.85f);

        // ドロップ品
        dropIfPresent(WEAPON_ID, 0.05, deathLoc);   // アークライト・バスター (5%)
        dropIfPresent(CHEST_ID, 0.08, deathLoc);    // laps_jacket (8%)
        dropIfPresent(BOOTS_ID, 0.08, deathLoc);    // laps_tread (8%)
        dropIfPresent("artifact_box", 0.02, deathLoc); // アーティファクトボックス (2%)
        dropIfPresent("abyss_shard", 0.30, deathLoc);  // 素材 (30%)
    }

    private void performShieldBash(LivingEntity target) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
        Vector dir = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        target.setVelocity(dir.multiply(0.6).setY(0.2));
        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 1);

        damageManager.processDamage(entity, target, DamageType.PHYSICAL, 12.0, Set.of("SHIELD_BASH"));
    }

    private void performCircularSwing() {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, entity.getLocation().add(0, 1, 0), 5, 1.5, 0.5, 1.5, 0.1);

        double dmg = 15.0;
        List<LivingEntity> nearby = entity.getWorld().getNearbyLivingEntities(entity.getLocation(), 3.5, 2.0, 3.5).stream()
                .filter(e -> !e.equals(entity))
                .filter(e -> !(e instanceof ArmorStand))
                .filter(e -> !(e instanceof VanguardSkeletonMob))
                .toList();

        for (LivingEntity victim : nearby) {
            damageManager.processDamage(entity, victim, DamageType.PHYSICAL, dmg, Set.of("SWEEP"));
            Vector knockbackDir = victim.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
            victim.setVelocity(knockbackDir.multiply(0.35).setY(0.15));
        }
    }

    private void performRiposte(LivingEntity attacker) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.5f);
        entity.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.2);

        damageManager.processDamage(entity, attacker, DamageType.PHYSICAL, 25.0, Set.of("RIPOSTE", "CRITICAL"));
    }

    private void performTacticalDash(LivingEntity target) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.5f);
        Vector dir = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        entity.setVelocity(dir.multiply(1.3).setY(0.12));

        entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation(), 15, 0.3, 0.3, 0.3, 0.08);
    }

    private LivingEntity getTarget() {
        return entity.getWorld().getNearbyLivingEntities(entity.getLocation(), 15.0, 8.0, 15.0).stream()
                .filter(e -> e instanceof Player p && p.getGameMode() != GameMode.SPECTATOR && p.getGameMode() != GameMode.CREATIVE)
                .findFirst().orElse(null);
    }

    @Override
    public double getBaseAttackDamage() {
        return BASE_ATTACK_DAMAGE;
    }

    @Override
    public double getBaseDefense() {
        return BASE_DEFENSE;
    }

    @Override
    public double getBaseMagicDefense() {
        return BASE_MAGIC_DEFENSE;
    }
}
