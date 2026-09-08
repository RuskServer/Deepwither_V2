package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailHelper;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

@Component
public class AncientRuinKnightMob extends CustomMob {

    private static final String MOB_ID = "ancient_ruin_knight";
    private static final String WEAPON_ID = "ether_shard_halberd";
    private static final String[] ARMOR_IDS = {
            "rusted_abyss_helmet",
            "rusted_abyss_chestplate",
            "rusted_abyss_leggings",
            "rusted_abyss_boots"
    };

    private static final double MAX_HP = 60.0;
    private static final double BASE_ATTACK_DAMAGE = 7.0;
    private static final double BASE_DEFENSE = 60.0;
    private static final double BASE_MAGIC_DEFENSE = 35.0;
    private static final double HEAVY_SLASH_DAMAGE = 12.0;
    private static final int EXP_REWARD = 120;
    private static final int HEAVY_SLASH_COOLDOWN = 180;
    private static final int HEAVY_SLASH_WINDUP = 16;
    private static final double HEAVY_SLASH_RANGE = 3.0;

    private final DamagePipelineManager damageManager;
    private final ItemManager itemManager;
    private int heavySlashCooldown = 80;
    private int heavySlashWindup;
    private Player heavySlashTarget;

    @Inject
    public AncientRuinKnightMob(CustomMobManager mobManager,
                                DamagePipelineManager damageManager,
                                ItemManager itemManager) {
        mobManager.registerMob(MOB_ID, EntityType.HUSK,
                () -> new AncientRuinKnightMob(mobManager, damageManager, itemManager));
        mobManager.registerDisplayName(MOB_ID, "錆朽ちた遺跡騎士");
        this.damageManager = damageManager;
        this.itemManager = itemManager;
    }

    @Override
    public void onSpawn() {
        setMaxHealth(MAX_HP);
        setExp(EXP_REWARD);
        setBaseName("錆朽ちた遺跡騎士");

        if (entity instanceof Husk husk) {
            husk.setBaby(false);
        }

        var speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.19);
        var knockbackResistance = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) knockbackResistance.setBaseValue(0.65);

        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;
        setEquipment(equipment::setHelmet, ARMOR_IDS[0]);
        setEquipment(equipment::setChestplate, ARMOR_IDS[1]);
        setEquipment(equipment::setLeggings, ARMOR_IDS[2]);
        setEquipment(equipment::setBoots, ARMOR_IDS[3]);
        setEquipment(equipment::setItemInMainHand, WEAPON_ID);
        equipment.setDropChance(EquipmentSlot.HEAD, 0.0f);
        equipment.setDropChance(EquipmentSlot.CHEST, 0.0f);
        equipment.setDropChance(EquipmentSlot.LEGS, 0.0f);
        equipment.setDropChance(EquipmentSlot.FEET, 0.0f);
        equipment.setDropChance(EquipmentSlot.HAND, 0.0f);
    }

    @Override
    public void onTick() {
        if (heavySlashWindup > 0) {
            heavySlashWindup--;
            if (heavySlashWindup % 4 == 0) {
                entity.getWorld().spawnParticle(
                        Particle.CRIT, entity.getEyeLocation(), 5,
                        0.2, 0.2, 0.2, 0.02);
            }
            if (heavySlashWindup == 0) executeHeavySlash();
            return;
        }

        if (heavySlashCooldown > 0) heavySlashCooldown--;
        if (heavySlashCooldown > 0 || ticksLived % 5 != 0) return;

        Player target = getNearestPlayer(HEAVY_SLASH_RANGE);
        if (target != null && entity.hasLineOfSight(target)) {
            startHeavySlash(target);
        }
    }

    @Override
    public void onDeath() {
        Location location = getLocation();
        location.getWorld().spawnParticle(
                Particle.SMOKE, location.clone().add(0, 1, 0),
                28, 0.5, 0.7, 0.5, 0.04);
        location.getWorld().playSound(
                location, Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 0.65f);

        if (RANDOM.nextDouble() < 0.18) {
            dropGenerated(ARMOR_IDS[RANDOM.nextInt(ARMOR_IDS.length)]);
        }
        dropGenerated(WEAPON_ID, 0.035);
        dropGenerated("abyss_shard", 0.30);
        dropGenerated("artifact_box", 0.0075);
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

    private void startHeavySlash(Player target) {
        heavySlashTarget = target;
        heavySlashWindup = HEAVY_SLASH_WINDUP;
        Vector direction = target.getLocation().toVector()
                .subtract(getLocation().toVector()).setY(0);
        if (direction.lengthSquared() > 0.01) {
            TrailHelper.spawnCone(
                    entity.getEyeLocation(), direction.normalize(),
                    HEAVY_SLASH_RANGE, 65.0, Color.fromRGB(0xB88955), 8, 5, 3);
        }
        entity.getWorld().playSound(
                getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.6f);
    }

    private void executeHeavySlash() {
        Player target = heavySlashTarget;
        heavySlashTarget = null;
        heavySlashCooldown = HEAVY_SLASH_COOLDOWN + RANDOM.nextInt(41);
        if (target == null || !target.isOnline() || target.isDead()) return;

        Vector forward = target.getLocation().toVector()
                .subtract(getLocation().toVector()).setY(0);
        if (forward.lengthSquared() < 0.01) return;
        forward.normalize();

        for (Player player : entity.getWorld().getPlayers()) {
            Vector offset = player.getLocation().toVector()
                    .subtract(getLocation().toVector());
            if (offset.lengthSquared() > HEAVY_SLASH_RANGE * HEAVY_SLASH_RANGE) continue;
            Vector horizontal = offset.clone().setY(0);
            if (horizontal.lengthSquared() < 0.01 || horizontal.normalize().dot(forward) >= 0.55) {
                damageManager.processDamage(
                        entity, player, DamageType.PHYSICAL, HEAVY_SLASH_DAMAGE,
                        null, "ancient_ruin_knight_heavy_slash", 500L);
                Vector knockback = horizontal.lengthSquared() < 0.01
                        ? forward.clone()
                        : horizontal.normalize();
                knockback.multiply(0.55).setY(0.18);
                player.setVelocity(player.getVelocity().add(knockback));
            }
        }

        Location impact = getLocation().clone().add(forward.multiply(1.4)).add(0, 0.4, 0);
        impact.getWorld().spawnParticle(
                Particle.CRIT, impact, 28, 0.8, 0.35, 0.8, 0.12);
        impact.getWorld().playSound(
                impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.55f);
    }

    private void setEquipment(java.util.function.Consumer<ItemStack> setter, String itemId) {
        ItemStack item = itemManager.generate(itemId);
        if (item != null) setter.accept(item);
    }

    private void dropGenerated(String itemId, double chance) {
        if (RANDOM.nextDouble() < chance) dropGenerated(itemId);
    }

    private void dropGenerated(String itemId) {
        ItemStack item = itemManager.generate(itemId);
        if (item != null) {
            getLocation().getWorld().dropItemNaturally(getLocation(), item);
        }
    }

    private Player getNearestPlayer(double radius) {
        List<Player> nearby = entity.getWorld().getPlayers().stream()
                .filter(player -> !player.isDead()
                        && player.getLocation().distanceSquared(getLocation()) <= radius * radius)
                .sorted((a, b) -> Double.compare(
                        a.getLocation().distanceSquared(getLocation()),
                        b.getLocation().distanceSquared(getLocation())))
                .toList();
        return combatTarget(nearby.isEmpty() ? null : nearby.get(0));
    }
}
