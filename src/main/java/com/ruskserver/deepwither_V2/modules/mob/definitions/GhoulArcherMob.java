package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;

@Component
public class GhoulArcherMob extends CustomMob {

    private static final String MOB_ID = "ghoul_archer";
    private static final String BOW_ID = "ghoulbone_bow";

    private static final double MAX_HP = 24.0;
    private static final double MELEE_DAMAGE = 2.0;
    private static final int EXP_REWARD = 80;

    private static final double SHOOT_RANGE = 14.0;
    private static final double KEEP_AWAY_RANGE = 4.0;
    private static final double ARROW_SPEED = 1.75;
    private static final int SHOOT_COOLDOWN = 70;
    private static final int BACKSTEP_COOLDOWN = 90;

    private int shootCooldown = 40;
    private int backstepCooldown = 20;

    private final DamagePipelineManager damageManager;
    private final ItemManager itemManager;
    private final NamespacedKey bowItemKey;

    @Inject
    public GhoulArcherMob(CustomMobManager mobManager, DamagePipelineManager damageManager,
                          ItemManager itemManager, Deepwither_V2 plugin) {
        mobManager.registerMob(MOB_ID, EntityType.ZOMBIE,
                () -> new GhoulArcherMob(mobManager, damageManager, itemManager, plugin));
        mobManager.registerDisplayName(MOB_ID, "グール・アーチャー");
        this.damageManager = damageManager;
        this.itemManager = itemManager;
        this.bowItemKey = new NamespacedKey(plugin, "bow_item_id");
    }

    @Override
    public void onSpawn() {
        setMaxHealth(MAX_HP);
        setExp(EXP_REWARD);
        setBaseName("グール・アーチャー");

        entity.customName(net.kyori.adventure.text.Component.text("グール・アーチャー")
                .color(net.kyori.adventure.text.format.TextColor.color(0x2E6B3E)));
        entity.setCustomNameVisible(true);

        if (entity instanceof Zombie zombie) {
            zombie.setBaby(false);
            zombie.setShouldBurnInDay(false);
        }

        var attackAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttr != null) attackAttr.setBaseValue(0.0);

        var speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.21);

        var equipment = entity.getEquipment();
        if (equipment != null) {
            var bow = itemManager.generate(BOW_ID);
            if (bow != null) {
                equipment.setItemInMainHand(bow);
                equipment.setItemInMainHandDropChance(0.0f);
            }
        }
    }

    @Override
    public void onTick() {
        if (shootCooldown > 0) shootCooldown--;
        if (backstepCooldown > 0) backstepCooldown--;
        if (ticksLived % 5 != 0) return;

        Player target = getNearestPlayer(SHOOT_RANGE);
        if (target == null) return;

        double distanceSquared = target.getLocation().distanceSquared(getLocation());
        if (distanceSquared <= KEEP_AWAY_RANGE * KEEP_AWAY_RANGE && backstepCooldown == 0) {
            backstepFrom(target);
            backstepCooldown = BACKSTEP_COOLDOWN;
            shootCooldown = Math.max(shootCooldown, 20);
            return;
        }

        if (shootCooldown == 0 && hasLineOfSight(target)) {
            shootAt(target);
            shootCooldown = SHOOT_COOLDOWN + RANDOM.nextInt(21);
        }
    }

    @Override
    public void onDeath() {
        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 24, 0.4, 0.5, 0.4, 0.03);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_DEATH, 1.0f, 0.8f);

        dropGenerated("ghoul_remnant", 0.25);
        dropGenerated("ghoul_viscera", 0.25);
        dropGenerated("ghoul_essence", 0.15);
        dropGenerated(BOW_ID, 0.08);
        dropGenerated("artifact_box", 0.01);
    }

    @Override
    public void onAttack(LivingEntity victim, org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (victim instanceof Player player) {
            damageManager.processDamage(entity, player, DamageType.PHYSICAL, MELEE_DAMAGE, null);
        }
    }

    @Override
    public double getBaseAttackDamage() {
        return MELEE_DAMAGE;
    }

    private void shootAt(Player target) {
        Location eye = entity.getEyeLocation();
        Location targetLoc = target.getEyeLocation();
        Vector direction = targetLoc.toVector().subtract(eye.toVector());
        if (direction.lengthSquared() < 0.01) return;

        Arrow arrow = entity.launchProjectile(Arrow.class, direction.normalize().multiply(ARROW_SPEED));
        arrow.setShooter(entity);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.getPersistentDataContainer().set(bowItemKey, PersistentDataType.STRING, BOW_ID);

        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.DUST, loc.add(0, 1.2, 0), 10, 0.25, 0.25, 0.25, 0,
                new Particle.DustOptions(Color.fromRGB(0x406B32), 1.2f));
        loc.getWorld().playSound(loc, Sound.ENTITY_SKELETON_SHOOT, 0.9f, 0.75f);
    }

    private void backstepFrom(Player target) {
        Vector away = getLocation().toVector().subtract(target.getLocation().toVector());
        if (away.lengthSquared() < 0.01) return;
        away.setY(0).normalize().multiply(0.75).setY(0.25);
        entity.setVelocity(away);

        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 0.4, 0), 18, 0.35, 0.25, 0.35, 0.04);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_STEP, 0.8f, 1.3f);
    }

    private boolean hasLineOfSight(Player target) {
        return entity.hasLineOfSight(target);
    }

    private void dropGenerated(String itemId, double chance) {
        if (RANDOM.nextDouble() >= chance) return;
        var item = itemManager.generate(itemId);
        if (item != null) {
            getLocation().getWorld().dropItemNaturally(getLocation(), item);
        }
    }

    private Player getNearestPlayer(double radius) {
        List<Player> nearby = entity.getWorld().getPlayers().stream()
                .filter(p -> !p.isDead() && p.getLocation().distanceSquared(getLocation()) <= radius * radius)
                .sorted((a, b) -> Double.compare(
                        a.getLocation().distanceSquared(getLocation()),
                        b.getLocation().distanceSquared(getLocation())))
                .toList();
        return nearby.isEmpty() ? null : nearby.get(0);
    }
}
