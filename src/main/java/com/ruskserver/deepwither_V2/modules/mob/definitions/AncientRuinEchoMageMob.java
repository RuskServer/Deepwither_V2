package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.mob.service.MobMagicProjectileService;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailHelper;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

@Component
public class AncientRuinEchoMageMob extends CustomMob {

    private static final String MOB_ID = "ancient_ruin_echo_mage";
    private static final String STAFF_ID = "astral_resonance_ru";
    private static final double MAX_HP = 22.0;
    private static final int EXP_REWARD = 140;
    private static final double TARGET_RANGE = 18.0;
    private static final double BOLT_DAMAGE = 3.8;
    private static final double BOLT_SPEED = 0.75;
    private static final int BOLT_COOLDOWN = 70;
    private static final int BOLT_WINDUP = 12;
    private static final double RUNE_DAMAGE = 5.0;
    private static final double RUNE_RADIUS = 2.5;
    private static final int RUNE_COOLDOWN = 220;
    private static final int RUNE_WINDUP = 24;
    private static final int BACKSTEP_COOLDOWN = 80;

    private final DamagePipelineManager damageManager;
    private final MobMagicProjectileService projectileService;
    private final ItemManager itemManager;
    private int boltCooldown = 35;
    private int runeCooldown = 120;
    private int backstepCooldown = 20;
    private CastType castType;
    private int castTicks;
    private Player castTarget;
    private Location runeLocation;

    @Inject
    public AncientRuinEchoMageMob(CustomMobManager mobManager,
                                  DamagePipelineManager damageManager,
                                  MobMagicProjectileService projectileService,
                                  ItemManager itemManager) {
        mobManager.registerMob(MOB_ID, EntityType.STRAY,
                () -> new AncientRuinEchoMageMob(
                        mobManager, damageManager, projectileService, itemManager));
        mobManager.registerDisplayName(MOB_ID, "遺跡の残響術師");
        this.damageManager = damageManager;
        this.projectileService = projectileService;
        this.itemManager = itemManager;
    }

    @Override
    public void onSpawn() {
        setMaxHealth(MAX_HP);
        setExp(EXP_REWARD);
        setBaseName("遺跡の残響術師");

        var attack = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(0.0);
        var speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.22);

        var equipment = entity.getEquipment();
        if (equipment != null) {
            ItemStack staff = itemManager.generate(STAFF_ID);
            if (staff != null) equipment.setItemInMainHand(staff);
            equipment.setDropChance(EquipmentSlot.HAND, 0.0f);
        }
    }

    @Override
    public void onTick() {
        if (castType != null) {
            tickCast();
            return;
        }

        if (boltCooldown > 0) boltCooldown--;
        if (runeCooldown > 0) runeCooldown--;
        if (backstepCooldown > 0) backstepCooldown--;
        if (ticksLived % 5 != 0) return;

        Player target = getNearestPlayer(TARGET_RANGE);
        if (target == null) return;

        double distanceSquared = target.getLocation().distanceSquared(getLocation());
        if (distanceSquared <= 16.0 && backstepCooldown == 0) {
            backstepFrom(target);
            backstepCooldown = BACKSTEP_COOLDOWN;
            return;
        }

        if (!entity.hasLineOfSight(target)) return;
        if (runeCooldown == 0 && distanceSquared <= 14.0 * 14.0) {
            startRune(target);
        } else if (boltCooldown == 0) {
            startBolt(target);
        }
    }

    @Override
    public void onDeath() {
        Location location = getLocation();
        location.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK, location.clone().add(0, 1, 0),
                30, 0.6, 0.8, 0.6, 0.12);
        location.getWorld().playSound(
                location, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 1.5f);

        dropGenerated(STAFF_ID, 0.05);
        dropGenerated("abyss_shard", 0.25);
        dropGenerated("artifact_box", 0.01);
    }

    private void startBolt(Player target) {
        castType = CastType.BOLT;
        castTicks = BOLT_WINDUP;
        castTarget = target;
        TrailHelper.spawnLine(
                entity.getEyeLocation(), target.getEyeLocation(),
                Color.fromRGB(0xA978FF), 12);
        entity.getWorld().playSound(
                getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 1.4f);
    }

    private void startRune(Player target) {
        castType = CastType.RUNE;
        castTicks = RUNE_WINDUP;
        castTarget = target;
        runeLocation = target.getLocation().clone().add(0, 0.08, 0);
        showRune();
        entity.getWorld().playSound(
                runeLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.7f);
    }

    private void tickCast() {
        castTicks--;
        if (castType == CastType.BOLT && castTicks % 3 == 0) {
            entity.getWorld().spawnParticle(
                    Particle.ENCHANT, entity.getEyeLocation(), 5,
                    0.15, 0.15, 0.15, 0.05);
        } else if (castType == CastType.RUNE && castTicks % 6 == 0) {
            showRune();
        }

        if (castTicks > 0) return;

        if (castType == CastType.BOLT) {
            finishBolt();
        } else {
            finishRune();
        }
        castType = null;
        castTarget = null;
        runeLocation = null;
    }

    private void finishBolt() {
        Player target = castTarget;
        boltCooldown = BOLT_COOLDOWN + RANDOM.nextInt(31);
        if (target == null || !target.isOnline() || target.isDead()
                || !entity.hasLineOfSight(target)) {
            return;
        }
        projectileService.launch(
                entity, target, BOLT_SPEED, BOLT_DAMAGE,
                "ancient_ruin_echo_bolt", Set.of("lightning"));
        entity.getWorld().playSound(
                getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.65f, 1.7f);
    }

    private void finishRune() {
        runeCooldown = RUNE_COOLDOWN + RANDOM.nextInt(61);
        if (runeLocation == null || runeLocation.getWorld() == null) return;

        for (Player player : runeLocation.getWorld().getPlayers()) {
            if (player.isDead()
                    || player.getLocation().distanceSquared(runeLocation) > RUNE_RADIUS * RUNE_RADIUS) {
                continue;
            }
            damageManager.processDamage(
                    entity, player, DamageType.MAGIC, RUNE_DAMAGE,
                    Set.of("lightning"), "ancient_ruin_echo_rune", 500L);
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, true));
        }

        runeLocation.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK, runeLocation.clone().add(0, 0.4, 0),
                50, 1.1, 0.35, 1.1, 0.18);
        runeLocation.getWorld().playSound(
                runeLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.4f);
    }

    private void showRune() {
        if (runeLocation == null) return;
        TrailCircleHelper.spawnCircle(
                runeLocation, RUNE_RADIUS, Color.fromRGB(0x8E63CE), 6, 24);
    }

    private void backstepFrom(Player target) {
        Vector away = getLocation().toVector()
                .subtract(target.getLocation().toVector()).setY(0);
        if (away.lengthSquared() < 0.01) return;
        entity.setVelocity(away.normalize().multiply(0.7).setY(0.22));
        entity.getWorld().spawnParticle(
                Particle.PORTAL, getLocation().clone().add(0, 0.6, 0),
                16, 0.3, 0.4, 0.3, 0.15);
    }

    private void dropGenerated(String itemId, double chance) {
        if (RANDOM.nextDouble() >= chance) return;
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
        return nearby.isEmpty() ? null : nearby.get(0);
    }

    private enum CastType {
        BOLT,
        RUNE
    }
}
