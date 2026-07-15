package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailHelper;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

@Component
public class FrostPilgrimBoss extends CustomMob {

    private static final String MOB_ID = "frost_pilgrim";

    private static final double BASE_HP = 20000.0;
    private static final double BASE_DEFENSE = 150.0;
    private static final double BASE_MAGIC_DEFENSE = 200.0;
    private static final int EXP_REWARD = 5000;

    private static final double PHASE_2_THRESHOLD = 0.60;
    private static final double PHASE_3_THRESHOLD = 0.30;

    private static final int ICE_BOLT_COOLDOWN = 60;
    private static final int FROST_NOVA_COOLDOWN = 200;
    private static final int ICE_PILLAR_COOLDOWN = 300;
    private static final int GLACIAL_CHARGE_COOLDOWN = 400;
    private static final int BLIZZARD_COOLDOWN = 600;

    private static final int P3_ICE_BOLT_COOLDOWN = 35;
    private static final int P3_FROST_NOVA_COOLDOWN = 140;
    private static final int P3_ICE_PILLAR_COOLDOWN = 200;
    private static final int P3_GLACIAL_CHARGE_COOLDOWN = 300;
    private static final int P3_BLIZZARD_COOLDOWN = 500;

    private static final double ICE_BOLT_RANGE = 24.0;
    private static final double FROST_NOVA_RADIUS = 6.0;
    private static final double ICE_PILLAR_RANGE = 20.0;
    private static final double GLACIAL_CHARGE_RANGE = 18.0;
    private static final double BLIZZARD_RADIUS = 10.0;
    private static final double FREEZING_AURA_RADIUS = 5.0;
    private static final double P3_FREEZING_AURA_RADIUS = 7.0;

    private static final double ICE_BOLT_DAMAGE_RATIO = 1.5;
    private static final double FROST_NOVA_DAMAGE_RATIO = 1.2;
    private static final double ICE_PILLAR_DAMAGE = 60.0;
    private static final double GLACIAL_CHARGE_DAMAGE_RATIO = 2.0;
    private static final double GLACIAL_CHARGE_HIT_RADIUS = 2.5;
    private static final double GLACIAL_CHARGE_POWER = 1.5;
    private static final double BLIZZARD_DAMAGE_PER_TICK = 10.0;
    private static final double FREEZING_AURA_DAMAGE_PER_TICK = 8.0;

    private static final Color ICE_BLUE = Color.fromRGB(0x8FD4E6);
    private static final Color ICE_WHITE = Color.fromRGB(0xFFFFFF);
    private static final Color ICE_DARK = Color.fromRGB(0x4A90D9);
    private static final Color ICE_PALE = Color.fromRGB(0xB0E0E6);

    private int iceBoltCooldown = 40;
    private int frostNovaCooldown = 100;
    private int icePillarCooldown = 120;
    private int glacialChargeCooldown = 200;
    private int blizzardCooldown = 300;
    private int freezingAuraTick = 0;

    private boolean windingFrostNova = false;
    private int frostNovaWindup = 0;

    private boolean windingGlacialCharge = false;
    private int glacialChargeWindup = 0;
    private Player glacialChargeTarget = null;
    private boolean glacialCharging = false;

    private boolean blizzardActive = false;
    private int blizzardTicks = 0;
    private Location blizzardCenter = null;
    private static final int BLIZZARD_DURATION = 200;

    private int currentPhase = 1;
    private boolean phaseTransitioning = false;
    private int phaseTransitionTicks = 0;

    private final DamagePipelineManager damageManager;
    private final StatManager statManager;
    private final VirtualHealthManager healthManager;
    private final ItemManager itemManager;
    private final JavaPlugin plugin;

    @Inject
    public FrostPilgrimBoss(CustomMobManager mobManager, DamagePipelineManager damageManager,
                            StatManager statManager, VirtualHealthManager healthManager,
                            ItemManager itemManager, JavaPlugin plugin) {
        mobManager.registerMob(MOB_ID, EntityType.STRAY,
                () -> new FrostPilgrimBoss(mobManager, damageManager, statManager, healthManager, itemManager, plugin));
        mobManager.registerDisplayName(MOB_ID, "氷結の巡礼者");
        this.damageManager = damageManager;
        this.statManager = statManager;
        this.healthManager = healthManager;
        this.itemManager = itemManager;
        this.plugin = plugin;
    }

    @Override
    public void onSpawn() {
        this.baseMaxHealth = BASE_HP;
        setMaxHealth(BASE_HP);
        setExp(EXP_REWARD);

        entity.customName(net.kyori.adventure.text.Component.text("氷結の巡礼者")
                .color(net.kyori.adventure.text.format.TextColor.color(0x8FD4E6)));
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);

        var attackAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttr != null) attackAttr.setBaseValue(0.0);

        var speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.15);

        var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) maxHealthAttr.setBaseValue(BASE_HP);
        entity.setHealth(BASE_HP);

        statManager.setModifier(uuid, StatType.DEFENSE, "boss_defense", BASE_DEFENSE, ModifierType.ADDITIVE);
        statManager.setModifier(uuid, StatType.MAGIC_DEFENSE, "boss_magic_defense", BASE_MAGIC_DEFENSE, ModifierType.ADDITIVE);
    }

    @Override
    public void onTick() {
        if (entity == null || !entity.isValid() || isDead()) return;

        double hpRatio = getHealth() / getMaxHealth();
        int newPhase = hpRatio <= PHASE_3_THRESHOLD ? 3 : hpRatio <= PHASE_2_THRESHOLD ? 2 : 1;

        if (newPhase > currentPhase) {
            startPhaseTransition(newPhase);
        }

        if (phaseTransitioning) {
            tickPhaseTransition();
            return;
        }

        currentPhase = newPhase;

        if (glacialCharging) {
            tickGlacialChargeMove();
            return;
        }

        if (windingFrostNova) {
            tickFrostNovaWindup();
            return;
        }

        if (windingGlacialCharge) {
            tickGlacialChargeWindup();
            return;
        }

        if (blizzardActive) {
            tickBlizzard();
        }

        if (currentPhase >= 2) {
            tickFreezingAura();
        }

        if (iceBoltCooldown > 0) iceBoltCooldown--;
        if (frostNovaCooldown > 0) frostNovaCooldown--;
        if (icePillarCooldown > 0) icePillarCooldown--;
        if (glacialChargeCooldown > 0) glacialChargeCooldown--;
        if (blizzardCooldown > 0) blizzardCooldown--;

        if (ticksLived % 5 != 0) return;

        boolean p3 = currentPhase == 3;
        if (p3 && blizzardCooldown <= 0 && !blizzardActive) {
            startBlizzard();
            return;
        }
        if (glacialChargeCooldown <= 0) {
            Player target = getNearestPlayer(GLACIAL_CHARGE_RANGE);
            if (target != null) {
                startGlacialCharge(target);
                return;
            }
        }
        if (frostNovaCooldown <= 0) {
            startFrostNova();
            return;
        }
        if (icePillarCooldown <= 0) {
            Player target = getNearestPlayer(ICE_PILLAR_RANGE);
            if (target != null) {
                castIcePillar(target);
                return;
            }
        }
        if (!p3 && blizzardCooldown <= 0 && !blizzardActive) {
            startBlizzard();
            return;
        }
        if (iceBoltCooldown <= 0) {
            Player target = getNearestPlayer(ICE_BOLT_RANGE);
            if (target != null) {
                castIceBolt(target);
            }
        }
    }

    @Override
    public void onDeath() {
        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 8, 2, 2, 2, 0.2);
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 100, 5, 3, 5, 0.5);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 2.0f, 0.5f);

        statManager.removeModifier(uuid, StatType.DEFENSE, "boss_defense");
        statManager.removeModifier(uuid, StatType.MAGIC_DEFENSE, "boss_magic_defense");

        dropIfPresent("frost_pilgrim_core", 1.0, loc);
        dropIfPresent("frost_crystal_shard", 0.6, loc);
        dropIfPresent("eternal_ice_shard", 0.3, loc);
        dropIfPresent("artifact_box", 0.05, loc);
        dropIfPresent("pilgrim_frost_robe", 0.02, loc);
    }

    @Override
    public double getBaseAttackDamage() {
        return 0.0;
    }

    @Override
    public void onAttack(LivingEntity victim, org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (victim instanceof Player player) {
            double magDamage = statManager.getTotalStat(entity, StatType.MAGIC_DAMAGE);
            damageManager.processDamage(entity, player, DamageType.MAGIC, magDamage * 0.5, null);
        }
    }

    private void startPhaseTransition(int newPhase) {
        phaseTransitioning = true;
        phaseTransitionTicks = 40;
        currentPhase = newPhase;

        Location loc = getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);

        if (newPhase == 2) {
            TrailCircleHelper.spawnCircle(loc.add(0, 0.1, 0), 8.0, ICE_DARK, 40, 48);
            loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 80, 4, 2, 4, 0.3);
        } else if (newPhase == 3) {
            TrailCircleHelper.spawnCircle(loc.add(0, 0.1, 0), 10.0, ICE_WHITE, 40, 48);
            loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 120, 6, 3, 6, 0.5);
            loc.getWorld().spawnParticle(Particle.CRIT, loc, 60, 4, 2, 4, 0.5);
        }
    }

    private void tickPhaseTransition() {
        if (phaseTransitionTicks > 0) {
            phaseTransitionTicks--;
            if (phaseTransitionTicks % 5 == 0) {
                Location loc = getLocation().add(0, 1, 0);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 10, 2, 1, 2, 0,
                        new Particle.DustOptions(ICE_BLUE, 1.5f));
            }
            return;
        }
        phaseTransitioning = false;
    }

    private void castIceBolt(Player target) {
        Location eye = entity.getEyeLocation();
        Vector dir = target.getEyeLocation().subtract(eye).toVector();
        if (dir.lengthSquared() < 0.01) return;
        dir.normalize();

        TrailHelper.spawnLine(eye, target.getEyeLocation(), ICE_BLUE, 15);
        eye.getWorld().playSound(eye, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.5f);

        if (target.isOnline() && !target.isDead()) {
            double magDamage = statManager.getTotalStat(entity, StatType.MAGIC_DAMAGE);
            double damage = magDamage * ICE_BOLT_DAMAGE_RATIO;
            damageManager.processDamage(entity, target, DamageType.MAGIC, damage, Set.of("ice"));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
        }

        target.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(ICE_WHITE, 1.8f));

        iceBoltCooldown = currentPhase == 3 ? P3_ICE_BOLT_COOLDOWN : ICE_BOLT_COOLDOWN;
    }

    private void startFrostNova() {
        Location loc = getLocation().add(0, 0.5, 0);
        TrailCircleHelper.spawnCircle(loc, FROST_NOVA_RADIUS, ICE_WHITE, 20, 36);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 0.4f);

        windingFrostNova = true;
        frostNovaWindup = 20;
    }

    private void tickFrostNovaWindup() {
        if (frostNovaWindup > 0) {
            frostNovaWindup--;
            if (frostNovaWindup % 5 == 0) {
                Location loc = getLocation().add(0, 0.5, 0);
                TrailCircleHelper.spawnCircle(loc, FROST_NOVA_RADIUS, ICE_PALE, 10, 36);
            }
            return;
        }

        windingFrostNova = false;
        Location loc = getLocation();

        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.add(0, 1, 0), 60, 3, 1, 3, 0.3);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 40, 3, 1, 3, 0.4);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);

        double magDamage = statManager.getTotalStat(entity, StatType.MAGIC_DAMAGE);
        double damage = magDamage * FROST_NOVA_DAMAGE_RATIO;

        for (Player p : loc.getWorld().getPlayers()) {
            if (p.isDead() || !p.isOnline()) continue;
            if (p.getLocation().distance(loc) <= FROST_NOVA_RADIUS) {
                damageManager.processDamage(entity, p, DamageType.MAGIC, damage, Set.of("ice"));
                Vector kb = p.getLocation().subtract(loc).toVector();
                kb.setY(0.3);
                if (kb.lengthSquared() > 0.01) kb.normalize().multiply(1.5);
                p.setVelocity(kb);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            }
        }

        frostNovaCooldown = currentPhase == 3 ? P3_FROST_NOVA_COOLDOWN : FROST_NOVA_COOLDOWN;
    }

    private void castIcePillar(Player target) {
        Location targetLoc = target.getLocation();
        World world = targetLoc.getWorld();

        TrailCircleHelper.spawnCircle(targetLoc.clone().add(0, 0.1, 0), 1.5, ICE_WHITE, 30, 24);
        world.playSound(targetLoc, Sound.BLOCK_SNOW_BREAK, 0.8f, 0.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!entity.isValid()) return;
            world.spawnParticle(Particle.SNOWFLAKE, targetLoc.add(0, 1, 0), 40, 1.5, 2, 1.5, 0.2);
            world.spawnParticle(Particle.DUST, targetLoc, 30, 1.5, 2, 1.5, 0,
                    new Particle.DustOptions(ICE_WHITE, 2.0f));
            world.playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.7f);

            for (Player p : world.getPlayers()) {
                if (p.isDead() || !p.isOnline()) continue;
                if (p.getLocation().distance(targetLoc) <= 3.0) {
                    damageManager.processDamage(entity, p, DamageType.MAGIC, ICE_PILLAR_DAMAGE, Set.of("ice"));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                }
            }
        }, 30L);

        icePillarCooldown = currentPhase == 3 ? P3_ICE_PILLAR_COOLDOWN : ICE_PILLAR_COOLDOWN;
    }

    private void startGlacialCharge(Player target) {
        Location from = getLocation().add(0, 0.5, 0);
        Vector dir = target.getLocation().subtract(getLocation()).toVector();
        if (dir.lengthSquared() < 0.01) return;
        dir.setY(0).normalize();
        Location to = from.clone().add(dir.multiply(GLACIAL_CHARGE_RANGE));
        TrailHelper.spawnLine(from, to, ICE_DARK, 15);

        getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_STRAY_AMBIENT, 1.0f, 0.5f);

        windingGlacialCharge = true;
        glacialChargeWindup = 15;
        glacialChargeTarget = target;
    }

    private void tickGlacialChargeWindup() {
        if (glacialChargeWindup > 0) {
            glacialChargeWindup--;
            return;
        }

        Player target = glacialChargeTarget;
        windingGlacialCharge = false;

        if (target == null || !target.isOnline() || target.isDead()) {
            glacialChargeTarget = null;
            glacialChargeCooldown = 60;
            return;
        }

        Vector dir = target.getLocation().subtract(getLocation()).toVector();
        if (dir.lengthSquared() < 0.01) {
            glacialChargeCooldown = 60;
            glacialChargeTarget = null;
            return;
        }
        dir.setY(0).normalize().multiply(GLACIAL_CHARGE_POWER);
        entity.setVelocity(dir);

        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.add(0, 0.5, 0), 30, 0.5, 0.3, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_STRAY_HURT, 1.2f, 0.6f);

        glacialCharging = true;
        glacialChargeCooldown = currentPhase == 3 ? P3_GLACIAL_CHARGE_COOLDOWN : GLACIAL_CHARGE_COOLDOWN;
    }

    private void tickGlacialChargeMove() {
        if (entity.isOnGround() || ticksLived % 3 == 0) {
            Location loc = getLocation();
            loc.getWorld().spawnParticle(Particle.DUST, loc.add(0, 0.3, 0), 3, 0.2, 0.1, 0.2, 0,
                    new Particle.DustOptions(ICE_BLUE, 1.2f));
        }

        if (!entity.isOnGround()) return;

        glacialCharging = false;
        Player target = glacialChargeTarget;
        glacialChargeTarget = null;

        if (target != null && target.isOnline() && !target.isDead()) {
            double dist = target.getLocation().distance(getLocation());
            if (dist <= GLACIAL_CHARGE_HIT_RADIUS) {
                double magDamage = statManager.getTotalStat(entity, StatType.MAGIC_DAMAGE);
                double damage = magDamage * GLACIAL_CHARGE_DAMAGE_RATIO;
                damageManager.processDamage(entity, target, DamageType.MAGIC, damage, Set.of("ice"));

                Vector kb = target.getLocation().subtract(getLocation()).toVector();
                kb.setY(0.4);
                if (kb.lengthSquared() > 0.01) kb.normalize().multiply(2.0);
                target.setVelocity(kb);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
            }
        }

        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.CRIT, loc.add(0, 0.5, 0), 20, 0.8, 0.5, 0.8, 0.3);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
    }

    private void startBlizzard() {
        blizzardActive = true;
        blizzardTicks = BLIZZARD_DURATION;
        blizzardCenter = getLocation();

        blizzardCenter.getWorld().playSound(blizzardCenter, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);

        TrailCircleHelper.spawnCircle(blizzardCenter.clone().add(0, 3, 0), BLIZZARD_RADIUS, ICE_WHITE, 40, 48,
                new Vector(0, 1, 0), 0, 2.0);

        TrailCircleHelper.spawnCircle(blizzardCenter.clone().add(0, 0.1, 0), BLIZZARD_RADIUS, ICE_PALE, 40, 48);

        blizzardCooldown = currentPhase == 3 ? P3_BLIZZARD_COOLDOWN : BLIZZARD_COOLDOWN;
    }

    private void tickBlizzard() {
        if (blizzardTicks <= 0) {
            blizzardActive = false;
            blizzardCenter = null;
            return;
        }
        blizzardTicks--;

        if (blizzardTicks % 10 != 0) return;

        World world = blizzardCenter.getWorld();
        for (int i = 0; i < 8; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * BLIZZARD_RADIUS;
            double x = blizzardCenter.getX() + r * Math.cos(angle);
            double z = blizzardCenter.getZ() + r * Math.sin(angle);
            double y = blizzardCenter.getY() + 5 + RANDOM.nextDouble() * 5;
            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0.3, 0.3, 0.3, 0.05);
        }

        for (Player p : world.getPlayers()) {
            if (p.isDead() || !p.isOnline()) continue;
            if (p.getLocation().distance(blizzardCenter) <= BLIZZARD_RADIUS) {
                damageManager.processDamage(entity, p, DamageType.MAGIC, BLIZZARD_DAMAGE_PER_TICK, Set.of("ice"));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0));
            }
        }
    }

    private void tickFreezingAura() {
        freezingAuraTick++;
        if (freezingAuraTick % 40 != 0) return;

        double radius = currentPhase == 3 ? P3_FREEZING_AURA_RADIUS : FREEZING_AURA_RADIUS;
        Location loc = getLocation();

        if (ticksLived % 20 == 0) {
            TrailCircleHelper.spawnCircle(loc.clone().add(0, 0.1, 0), radius, ICE_PALE, 25, 32);
        }

        for (Player p : loc.getWorld().getPlayers()) {
            if (p.isDead() || !p.isOnline()) continue;
            if (p.getLocation().distance(loc) <= radius) {
                damageManager.processDamage(entity, p, DamageType.MAGIC, FREEZING_AURA_DAMAGE_PER_TICK, Set.of("ice"));
            }
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

    private boolean isDead() {
        return entity == null || entity.isDead() || !entity.isValid();
    }
}
