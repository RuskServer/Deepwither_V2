package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.combat.stagger.StaggerProfile;
import com.ruskserver.deepwither_V2.modules.combat.stagger.StaggerState;
import com.ruskserver.deepwither_V2.modules.combat.stagger.StaggerableBoss;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailHelper;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class FrostPilgrimBoss extends CustomMob implements StaggerableBoss {

    private static final String MOB_ID = "frost_pilgrim";

    private static final double BASE_HP = 20000.0;
    private static final double BASE_DEFENSE = 150.0;
    private static final double BASE_MAGIC_DEFENSE = 200.0;
    private static final int EXP_REWARD = 5000;

    private static final double PHASE_2_THRESHOLD = 0.60;
    private static final double PHASE_3_THRESHOLD = 0.30;
    private static final StaggerProfile STAGGER_PROFILE = new StaggerProfile(
            1500.0,
            70,
            160,
            60,
            6.0,
            1.2,
            300.0,
            1.0,
            0.85,
            0.75,
            0.5
    );

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

    private static final double INTRO_AUDIENCE_RADIUS = 36.0;
    private static final double INTRO_IMPACT_RADIUS = 6.0;
    private static final double INTRO_IMPACT_DAMAGE = 100.0;
    private static final int INTRO_FOCUS_TICKS = 20;
    private static final int INTRO_REVEAL_TICKS = 20;
    private static final int INTRO_DIVE_TICKS = 12;
    private static final int INTRO_CHARACTER_TICKS = 2;
    private static final int INTRO_POST_COMBAT_GRACE_TICKS = 50;
    private static final String INTRO_FLAVOR_TEXT = "「凍てつく祈りは、終わりを赦さない。」";

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
    private int glacialChargeTicks = 0;
    private Vector glacialChargeDirection = null;

    private boolean blizzardActive = false;
    private int blizzardTicks = 0;
    private Location blizzardCenter = null;
    private int actionGeneration = 0;
    private static final int BLIZZARD_DURATION = 200;

    private int currentPhase = 1;
    private boolean phaseTransitioning = false;
    private int phaseTransitionTicks = 0;
    private BossBar bossBar;
    private BossBar staggerBar;
    private final StaggerState staggerState = new StaggerState();

    private IntroPhase introPhase = IntroPhase.ACTIVE;
    private int introPhaseTicks = 0;
    private Location introGroundLocation;
    private Location introRevealLocation;
    private double introHeight = 0.0;
    private final Set<UUID> introAudience = new HashSet<>();

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

        bossBar = Bukkit.createBossBar(
                bossBarTitle(),
                BarColor.BLUE,
                BarStyle.SEGMENTED_10
        );
        bossBar.setProgress(1.0);
        bossBar.setVisible(false);
        staggerBar = Bukkit.createBossBar(
                "§e§l体勢",
                BarColor.YELLOW,
                BarStyle.SEGMENTED_10
        );
        staggerBar.setProgress(0.0);
        staggerBar.setVisible(false);

        if (entity.getWorld().getName().startsWith("dungeon_")) {
            initializeDungeonIntro();
        } else {
            activateImmediately();
        }
    }

    @Override
    public void onTick() {
        if (entity == null || !entity.isValid() || isDead()) {
            removeBossBar();
            return;
        }

        if (introPhase != IntroPhase.ACTIVE) {
            tickIntro();
            return;
        }

        if (ticksLived % 5 == 0) {
            updateBossBar();
        }

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
        if (staggerState.tick(STAGGER_PROFILE) == StaggerState.TickResult.RECOVERED) {
            onStaggerRecovered();
        }
        if (staggerState.isStaggered()) {
            entity.setVelocity(new Vector());
            return;
        }

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
        removeBossBar();
        introAudience.clear();

        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 8, 2, 2, 2, 0.2);
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 100, 5, 3, 5, 0.5);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 2.0f, 0.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.8f, 0.5f);
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, loc.clone().add(0, 0.15, 0),
                1.0, 12.0, ICE_WHITE, 24, 36
        );

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
        if (introPhase != IntroPhase.ACTIVE) {
            event.setCancelled(true);
            return;
        }
        if (victim instanceof Player player) {
            double magDamage = statManager.getTotalStat(entity, StatType.MAGIC_DAMAGE);
            damageManager.processDamage(entity, player, DamageType.MAGIC, magDamage * 0.5, null);
        }
    }

    private void startPhaseTransition(int newPhase) {
        cancelCurrentActionForStagger();
        staggerState.resetWithImmunity(STAGGER_PROFILE.recoveryImmunityTicks());
        phaseTransitioning = true;
        phaseTransitionTicks = 40;
        currentPhase = newPhase;
        entity.setAI(false);
        entity.setVelocity(new Vector());
        if (staggerBar != null) {
            staggerBar.removeAll();
            staggerBar.setVisible(false);
        }

        Location loc = getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f,
                newPhase == 3 ? 0.45f : 0.65f);

        if (bossBar != null) {
            bossBar.setTitle(bossBarTitle());
            bossBar.setColor(newPhase == 3 ? BarColor.WHITE : BarColor.BLUE);
            bossBar.addFlag(BarFlag.DARKEN_SKY);
        }

        if (newPhase == 2) {
            TrailCircleHelper.spawnCircle(loc.add(0, 0.1, 0), 8.0, ICE_DARK, 40, 48);
            loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 80, 4, 2, 4, 0.3);
        } else if (newPhase == 3) {
            TrailCircleHelper.spawnCircle(loc.add(0, 0.1, 0), 10.0, ICE_WHITE, 40, 48);
            TrailCircleHelper.spawnRadialBurstRing(
                    loc.clone(), 6.0, 4.0, ICE_WHITE, 24, 40,
                    new Vector(0, 1, 0), 0
            );
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
        entity.setAI(true);
        Location loc = getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.6f,
                currentPhase == 3 ? 0.45f : 0.65f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.55f);
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, loc.clone().add(0, 0.15, 0),
                1.0, currentPhase == 3 ? 11.0 : 8.0,
                currentPhase == 3 ? ICE_WHITE : ICE_BLUE,
                16, 32
        );
    }

    private void castIceBolt(Player target) {
        Location eye = entity.getEyeLocation();
        Vector dir = target.getEyeLocation().subtract(eye).toVector();
        if (dir.lengthSquared() < 0.01) return;
        dir.normalize();

        TrailHelper.spawnLine(eye, target.getEyeLocation(), ICE_BLUE, 15);
        TrailHelper.spawnBeam(
                eye, dir, eye.distance(target.getEyeLocation()),
                ICE_PALE, 8, 0.16
        );
        eye.getWorld().playSound(eye, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.5f);
        eye.getWorld().playSound(eye, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 1.7f);

        if (target.isOnline() && !target.isDead()) {
            double magDamage = statManager.getTotalStat(entity, StatType.MAGIC_DAMAGE);
            double damage = magDamage * ICE_BOLT_DAMAGE_RATIO;
            damageManager.processDamage(entity, target, DamageType.MAGIC, damage, Set.of("ice"));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
        }

        target.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(ICE_WHITE, 1.8f));
        TrailCircleHelper.spawnRadialBurstRing(
                target.getEyeLocation(), 0.5, 1.4, ICE_WHITE,
                8, 12, dir, 0
        );
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.9f, 1.65f);

        iceBoltCooldown = currentPhase == 3 ? P3_ICE_BOLT_COOLDOWN : ICE_BOLT_COOLDOWN;
    }

    private void startFrostNova() {
        Location loc = getLocation().add(0, 0.5, 0);
        TrailCircleHelper.spawnCircle(loc, FROST_NOVA_RADIUS, ICE_WHITE, 20, 36);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 0.4f);
        loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.75f);

        windingFrostNova = true;
        frostNovaWindup = 20;
    }

    private void tickFrostNovaWindup() {
        if (frostNovaWindup > 0) {
            frostNovaWindup--;
            if (frostNovaWindup % 5 == 0) {
                Location loc = getLocation().add(0, 0.5, 0);
                TrailCircleHelper.spawnCircle(loc, FROST_NOVA_RADIUS, ICE_PALE, 10, 36);
                if (frostNovaWindup == 10) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);
                }
            }
            return;
        }

        windingFrostNova = false;
        Location loc = getLocation();

        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.add(0, 1, 0), 60, 3, 1, 3, 0.3);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 40, 3, 1, 3, 0.4);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.8f, 0.55f);
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, getLocation().clone().add(0, 0.15, 0),
                0.8, FROST_NOVA_RADIUS + 1.5, ICE_WHITE, 10, 32
        );
        TrailCircleHelper.spawnRadialBurstRing(
                getLocation().clone().add(0, 0.4, 0),
                FROST_NOVA_RADIUS, 2.5, ICE_PALE,
                12, 36, new Vector(0, 1, 0), 0
        );

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
        int castGeneration = actionGeneration;

        TrailCircleHelper.spawnCircle(targetLoc.clone().add(0, 0.1, 0), 1.5, ICE_WHITE, 30, 24);
        world.playSound(targetLoc, Sound.BLOCK_SNOW_BREAK, 0.8f, 0.5f);
        world.playSound(targetLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.4f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!entity.isValid() || castGeneration != actionGeneration
                    || phaseTransitioning || staggerState.isStaggered()) {
                return;
            }
            Location pillarBase = targetLoc.clone();
            world.spawnParticle(Particle.SNOWFLAKE, pillarBase.clone().add(0, 2.5, 0), 70, 1.5, 2.5, 1.5, 0.2);
            world.spawnParticle(Particle.DUST, pillarBase.clone().add(0, 2.0, 0), 45, 1.5, 2, 1.5, 0,
                    new Particle.DustOptions(ICE_WHITE, 2.0f));
            TrailHelper.spawnBeam(
                    pillarBase.clone().add(0, 0.1, 0),
                    new Vector(0, 1, 0), 6.0, ICE_WHITE, 12, 0.45
            );
            TrailCircleHelper.spawnExpandingShockwave(
                    plugin, pillarBase.clone().add(0, 0.15, 0),
                    0.5, 3.5, ICE_PALE, 8, 20
            );
            world.playSound(pillarBase, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
            world.playSound(pillarBase, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.35f);

            for (Player p : world.getPlayers()) {
                if (p.isDead() || !p.isOnline()) continue;
                if (p.getLocation().distance(pillarBase) <= 3.0) {
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
        getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.2f, 0.55f);

        windingGlacialCharge = true;
        glacialChargeWindup = 20;
        glacialChargeTarget = target;
    }

    private void tickGlacialChargeWindup() {
        if (glacialChargeWindup > 0) {
            glacialChargeWindup--;
            if (glacialChargeWindup % 5 == 0) {
                Location loc = getLocation().clone().add(0, 0.25, 0);
                TrailCircleHelper.spawnCircle(
                        loc, 1.0 + (20 - glacialChargeWindup) * 0.04,
                        ICE_DARK, 6, 16
                );
                loc.getWorld().playSound(
                        loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                        0.7f, 0.55f + (20 - glacialChargeWindup) * 0.035f
                );
            }
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
        glacialChargeDirection = dir.setY(0).normalize();
        entity.setVelocity(glacialChargeDirection.clone().multiply(GLACIAL_CHARGE_POWER));

        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.add(0, 0.5, 0), 30, 0.5, 0.3, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_STRAY_HURT, 1.2f, 0.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.65f);

        glacialCharging = true;
        glacialChargeTicks = 14;
        glacialChargeCooldown = currentPhase == 3 ? P3_GLACIAL_CHARGE_COOLDOWN : GLACIAL_CHARGE_COOLDOWN;
    }

    private void tickGlacialChargeMove() {
        if (glacialChargeDirection == null || glacialChargeTicks <= 0) {
            finishGlacialCharge(null);
            return;
        }

        entity.setVelocity(glacialChargeDirection.clone().multiply(GLACIAL_CHARGE_POWER));
        Location loc = getLocation();
        Location trailEnd = loc.clone().subtract(glacialChargeDirection.clone().multiply(2.5));
        TrailHelper.spawnSegmentedLine(
                loc.clone().add(0, 0.8, 0),
                trailEnd.add(0, 0.8, 0),
                ICE_BLUE, 4, 5
        );
        loc.getWorld().spawnParticle(
                Particle.SNOWFLAKE, loc.clone().add(0, 0.6, 0),
                8, 0.45, 0.35, 0.45, 0.08
        );

        Player hit = loc.getWorld().getPlayers().stream()
                .filter(player -> player.isOnline() && !player.isDead())
                .filter(player -> player.getLocation().distanceSquared(loc)
                        <= GLACIAL_CHARGE_HIT_RADIUS * GLACIAL_CHARGE_HIT_RADIUS)
                .findFirst()
                .orElse(null);
        if (hit != null) {
            finishGlacialCharge(hit);
            return;
        }

        glacialChargeTicks--;
        if (glacialChargeTicks <= 0) {
            finishGlacialCharge(null);
        }
    }

    @Override
    public void onDamaged(LivingEntity attacker, org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (introPhase != IntroPhase.ACTIVE) {
            event.setCancelled(true);
        }
    }

    @Override
    public StaggerProfile getStaggerProfile() {
        return STAGGER_PROFILE;
    }

    @Override
    public StaggerState getStaggerState() {
        return staggerState;
    }

    @Override
    public boolean canReceiveStagger(DamageContext context) {
        if (introPhase != IntroPhase.ACTIVE || phaseTransitioning || isDead() || getHealth() <= 0.0) {
            return false;
        }

        double hpRatio = getMaxHealth() <= 0.0 ? 0.0 : getHealth() / getMaxHealth();
        int healthPhase = hpRatio <= PHASE_3_THRESHOLD ? 3 : hpRatio <= PHASE_2_THRESHOLD ? 2 : 1;
        return healthPhase <= currentPhase;
    }

    @Override
    public void onStaggerStarted() {
        cancelCurrentActionForStagger();
        entity.setAI(false);
        entity.setVelocity(new Vector());

        Location loc = getLocation();
        loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, 1.8f, 0.65f);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.45f);
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.1f, 0.7f);
        loc.getWorld().spawnParticle(
                Particle.BLOCK, loc.clone().add(0, 1.0, 0),
                55, 0.8, 1.0, 0.8, 0.15,
                org.bukkit.Material.PACKED_ICE.createBlockData()
        );
        loc.getWorld().spawnParticle(
                Particle.DUST, loc.clone().add(0, 1.0, 0),
                35, 0.9, 0.9, 0.9, 0.0,
                new Particle.DustOptions(ICE_WHITE, 1.4f)
        );
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, loc.clone().add(0, 0.15, 0),
                0.5, 4.5, ICE_PALE, 8, 24
        );
    }

    @Override
    public void onStaggerRecovered() {
        if (introPhase != IntroPhase.ACTIVE || phaseTransitioning || isDead()) {
            return;
        }

        entity.setAI(true);
        entity.setVelocity(new Vector());
        frostNovaCooldown = Math.max(frostNovaCooldown, 60);
        icePillarCooldown = Math.max(icePillarCooldown, 60);
        glacialChargeCooldown = Math.max(glacialChargeCooldown, 60);
        blizzardCooldown = Math.max(blizzardCooldown, 100);

        Location loc = getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.4f, 0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_STRAY_AMBIENT, 1.0f, 0.85f);
        loc.getWorld().spawnParticle(
                Particle.DUST, loc.clone().add(0, 1.0, 0),
                28, 0.7, 0.9, 0.7, 0.0,
                new Particle.DustOptions(ICE_BLUE, 1.2f)
        );
    }

    private void cancelCurrentActionForStagger() {
        actionGeneration++;
        windingFrostNova = false;
        frostNovaWindup = 0;
        windingGlacialCharge = false;
        glacialChargeWindup = 0;
        glacialChargeTarget = null;
        glacialCharging = false;
        glacialChargeTicks = 0;
        glacialChargeDirection = null;
        blizzardActive = false;
        blizzardTicks = 0;
        blizzardCenter = null;
        if (entity != null) {
            entity.setVelocity(new Vector());
        }
    }

    private void initializeDungeonIntro() {
        introGroundLocation = getLocation().clone();
        introHeight = findSafeIntroHeight(introGroundLocation);
        introRevealLocation = introGroundLocation.clone().add(0, introHeight, 0);
        introPhase = IntroPhase.WAITING;
        introPhaseTicks = 0;

        entity.setAI(false);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setInvisible(true);
        entity.setSilent(true);
        entity.setCollidable(false);
        entity.setCustomNameVisible(false);
        entity.teleport(introRevealLocation);
        entity.setVelocity(new Vector());
    }

    private double findSafeIntroHeight(Location ground) {
        for (int height = 6; height >= 3; height--) {
            Location feet = ground.clone().add(0, height, 0);
            if (feet.getBlock().isPassable() && feet.clone().add(0, 1, 0).getBlock().isPassable()) {
                return height;
            }
        }
        return 0.0;
    }

    private void tickIntro() {
        switch (introPhase) {
            case WAITING -> tickIntroWaiting();
            case FOCUS -> tickIntroFocus();
            case TYPEWRITER -> tickIntroTypewriter();
            case REVEAL -> tickIntroReveal();
            case DIVE -> tickIntroDive();
            case ACTIVE -> {
            }
        }
    }

    private void tickIntroWaiting() {
        entity.setVelocity(new Vector());
        if (ticksLived % 5 != 0) {
            return;
        }

        introAudience.clear();
        double radiusSquared = INTRO_AUDIENCE_RADIUS * INTRO_AUDIENCE_RADIUS;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.isOnline() && !player.isDead()
                    && player.getLocation().distanceSquared(introGroundLocation) <= radiusSquared) {
                introAudience.add(player.getUniqueId());
            }
        }
        if (introAudience.isEmpty()) {
            return;
        }

        introPhase = IntroPhase.FOCUS;
        introPhaseTicks = 0;
        World world = introGroundLocation.getWorld();
        world.playSound(introGroundLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.35f);
        world.playSound(introGroundLocation, Sound.BLOCK_POWDER_SNOW_BREAK, 1.4f, 0.45f);
    }

    private void tickIntroFocus() {
        if (!hasValidIntroAudience()) {
            resetIntroWaiting();
            return;
        }

        focusIntroAudience();
        if (introPhaseTicks % 5 == 0) {
            Location center = introGroundLocation.clone().add(0, 0.15, 0);
            TrailCircleHelper.spawnCircle(
                    center, 2.0 + introPhaseTicks * 0.08,
                    introPhaseTicks < 10 ? ICE_DARK : ICE_PALE, 8, 28
            );
            spawnAscendingIce(center, 8);
        }

        introPhaseTicks++;
        if (introPhaseTicks >= INTRO_FOCUS_TICKS) {
            introPhase = IntroPhase.TYPEWRITER;
            introPhaseTicks = 0;
        }
    }

    private void tickIntroTypewriter() {
        if (!hasValidIntroAudience()) {
            resetIntroWaiting();
            return;
        }

        if (introPhaseTicks % 2 == 0) {
            focusIntroAudience();
        }

        int codePointCount = INTRO_FLAVOR_TEXT.codePointCount(0, INTRO_FLAVOR_TEXT.length());
        int visibleCharacters = Math.min(
                codePointCount,
                introPhaseTicks / INTRO_CHARACTER_TICKS + 1
        );
        int endIndex = INTRO_FLAVOR_TEXT.offsetByCodePoints(0, visibleCharacters);
        net.kyori.adventure.text.Component message =
                net.kyori.adventure.text.Component.text(INTRO_FLAVOR_TEXT.substring(0, endIndex))
                .color(TextColor.color(0xBFEFFF));
        forEachIntroAudience(player -> player.sendActionBar(message));

        if (introPhaseTicks % 8 == 0) {
            float pitch = 0.8f + visibleCharacters * 0.025f;
            forEachIntroAudience(player -> player.playSound(
                    player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.45f, pitch
            ));
            spawnAscendingIce(introGroundLocation.clone().add(0, 0.2, 0), 5);
        }

        introPhaseTicks++;
        if (introPhaseTicks >= codePointCount * INTRO_CHARACTER_TICKS + 10) {
            beginIntroReveal();
        }
    }

    private void beginIntroReveal() {
        introPhase = IntroPhase.REVEAL;
        introPhaseTicks = 0;
        entity.setInvisible(false);
        entity.setSilent(false);
        entity.setCustomNameVisible(true);
        entity.teleport(introRevealLocation);

        World world = introGroundLocation.getWorld();
        world.playSound(introRevealLocation, Sound.ENTITY_STRAY_AMBIENT, 1.8f, 0.45f);
        world.playSound(introRevealLocation, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.6f, 0.55f);
        world.playSound(introGroundLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.4f, 0.4f);
        TrailCircleHelper.spawnCircle(
                introRevealLocation.clone().add(0, 1, 0), 2.2, ICE_WHITE, 30, 32
        );
        TrailCircleHelper.spawnCircle(
                introGroundLocation.clone().add(0, 0.15, 0), INTRO_IMPACT_RADIUS, ICE_DARK, 30, 48
        );
    }

    private void tickIntroReveal() {
        entity.setVelocity(new Vector());
        if (introPhaseTicks % 5 == 0) {
            TrailCircleHelper.spawnCircle(
                    introGroundLocation.clone().add(0, 0.15, 0),
                    INTRO_IMPACT_RADIUS, introPhaseTicks < 10 ? ICE_DARK : ICE_WHITE, 10, 48
            );
            spawnDescendingIce(introRevealLocation.clone().add(0, 1, 0), 10);
        }

        introPhaseTicks++;
        if (introPhaseTicks >= INTRO_REVEAL_TICKS) {
            if (introHeight <= 0.0) {
                impactAndActivate();
                return;
            }
            introPhase = IntroPhase.DIVE;
            introPhaseTicks = 0;
            introGroundLocation.getWorld().playSound(
                    introRevealLocation, Sound.ENTITY_STRAY_HURT, 1.5f, 0.45f
            );
        }
    }

    private void tickIntroDive() {
        introPhaseTicks++;
        double progress = Math.min(1.0, introPhaseTicks / (double) INTRO_DIVE_TICKS);
        double easedProgress = progress * progress;
        Location next = introRevealLocation.clone().add(0, -introHeight * easedProgress, 0);
        entity.teleport(next);
        entity.setVelocity(new Vector());

        Location trailStart = next.clone().add(0, 2.0, 0);
        TrailHelper.spawnSegmentedLine(
                trailStart, trailStart.clone().add(0, 2.5, 0), ICE_PALE, 3, 5
        );
        spawnDescendingIce(next.clone().add(0, 1.0, 0), 7);

        if (introPhaseTicks >= INTRO_DIVE_TICKS) {
            impactAndActivate();
        }
    }

    private void impactAndActivate() {
        entity.teleport(introGroundLocation);
        entity.setVelocity(new Vector());

        World world = introGroundLocation.getWorld();
        Location impact = introGroundLocation.clone().add(0, 0.15, 0);
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, impact, 0.8, INTRO_IMPACT_RADIUS + 1.5, ICE_WHITE, 10, 40
        );
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, impact.clone().add(0, 0.18, 0),
                1.4, INTRO_IMPACT_RADIUS, ICE_DARK, 12, 36
        );
        TrailCircleHelper.spawnRadialBurstRing(
                impact.clone().add(0, 0.4, 0),
                INTRO_IMPACT_RADIUS, 3.0, ICE_PALE, 20, 40,
                new Vector(0, 1, 0), 0
        );
        world.spawnParticle(Particle.SNOWFLAKE, impact, 90, 2.5, 0.8, 2.5, 0.35);
        world.spawnParticle(Particle.CRIT, impact, 55, 2.0, 0.5, 2.0, 0.45);
        world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.5f);
        world.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.7f, 0.55f);
        world.playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.7f, 0.45f);
        world.playSound(impact, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.65f, 1.55f);

        double radiusSquared = INTRO_IMPACT_RADIUS * INTRO_IMPACT_RADIUS;
        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.isDead()
                    || player.getLocation().distanceSquared(introGroundLocation) > radiusSquared) {
                continue;
            }
            damageManager.processDamage(
                    entity, player, DamageType.MAGIC, INTRO_IMPACT_DAMAGE, Set.of("ice", "boss_intro")
            );
            Vector knockback = player.getLocation().subtract(introGroundLocation).toVector();
            knockback.setY(0.38);
            if (knockback.lengthSquared() > 0.01) {
                knockback.normalize().multiply(1.25);
            }
            player.setVelocity(knockback);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 1));
        }

        introPhase = IntroPhase.ACTIVE;
        introPhaseTicks = 0;
        introAudience.clear();
        restoreActiveBossState();
        iceBoltCooldown = Math.max(iceBoltCooldown, INTRO_POST_COMBAT_GRACE_TICKS);
        frostNovaCooldown = Math.max(frostNovaCooldown, INTRO_POST_COMBAT_GRACE_TICKS + 30);
        icePillarCooldown = Math.max(icePillarCooldown, INTRO_POST_COMBAT_GRACE_TICKS + 50);
        bossBar.setVisible(true);
        updateBossBar();
    }

    private void resetIntroWaiting() {
        introPhase = IntroPhase.WAITING;
        introPhaseTicks = 0;
        introAudience.clear();
        entity.setInvisible(true);
        entity.setSilent(true);
        entity.setCustomNameVisible(false);
        entity.teleport(introRevealLocation);
        entity.setVelocity(new Vector());
    }

    private boolean hasValidIntroAudience() {
        introAudience.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            return player == null || !player.isOnline() || player.isDead()
                    || !player.getWorld().equals(entity.getWorld());
        });
        return !introAudience.isEmpty();
    }

    private void focusIntroAudience() {
        forEachIntroAudience(player -> player.lookAt(entity, LookAnchor.EYES, LookAnchor.EYES));
    }

    private void forEachIntroAudience(java.util.function.Consumer<Player> action) {
        for (UUID audienceId : Set.copyOf(introAudience)) {
            Player player = Bukkit.getPlayer(audienceId);
            if (player != null && player.isOnline() && !player.isDead()
                    && player.getWorld().equals(entity.getWorld())) {
                action.accept(player);
            }
        }
    }

    private void spawnAscendingIce(Location center, int count) {
        World world = center.getWorld();
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = 1.0 + RANDOM.nextDouble() * (INTRO_IMPACT_RADIUS - 1.0);
            Location particleLocation = center.clone().add(
                    Math.cos(angle) * radius,
                    RANDOM.nextDouble() * 1.5,
                    Math.sin(angle) * radius
            );
            world.spawnParticle(Particle.SNOWFLAKE, particleLocation, 0, 0, 1, 0, 0.08);
        }
    }

    private void spawnDescendingIce(Location center, int count) {
        World world = center.getWorld();
        for (int i = 0; i < count; i++) {
            Location particleLocation = center.clone().add(
                    (RANDOM.nextDouble() - 0.5) * 3.5,
                    RANDOM.nextDouble() * 2.0,
                    (RANDOM.nextDouble() - 0.5) * 3.5
            );
            world.spawnParticle(Particle.SNOWFLAKE, particleLocation, 0, 0, -1, 0, 0.12);
        }
    }

    private void activateImmediately() {
        introPhase = IntroPhase.ACTIVE;
        restoreActiveBossState();
        bossBar.setVisible(true);
        updateBossBar();

        Location spawnLoc = getLocation();
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, spawnLoc.clone().add(0, 0.15, 0),
                1.0, 8.0, ICE_PALE, 18, 28
        );
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 1.7f, 0.55f);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.4f, 0.65f);
    }

    private void restoreActiveBossState() {
        entity.setAI(true);
        entity.setGravity(true);
        entity.setInvulnerable(false);
        entity.setInvisible(false);
        entity.setSilent(false);
        entity.setCollidable(true);
        entity.setCustomNameVisible(true);
    }

    private enum IntroPhase {
        WAITING,
        FOCUS,
        TYPEWRITER,
        REVEAL,
        DIVE,
        ACTIVE
    }

    private void finishGlacialCharge(Player hitTarget) {
        glacialCharging = false;
        glacialChargeTicks = 0;
        glacialChargeDirection = null;
        glacialChargeTarget = null;

        if (hitTarget != null && hitTarget.isOnline() && !hitTarget.isDead()) {
            double magDamage = statManager.getTotalStat(entity, StatType.MAGIC_DAMAGE);
            double damage = magDamage * GLACIAL_CHARGE_DAMAGE_RATIO;
            damageManager.processDamage(entity, hitTarget, DamageType.MAGIC, damage, Set.of("ice"));

            Vector kb = hitTarget.getLocation().subtract(getLocation()).toVector();
            kb.setY(0.4);
            if (kb.lengthSquared() > 0.01) kb.normalize().multiply(2.0);
            hitTarget.setVelocity(kb);
            hitTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
        }

        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.5, 0), 40, 1.4, 0.8, 1.4, 0.35);
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 0.8, 0), 60, 1.8, 1.0, 1.8, 0.2);
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, loc.clone().add(0, 0.15, 0),
                0.7, 5.0, ICE_DARK, 9, 24
        );
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.55f);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.7f);
    }

    private void startBlizzard() {
        blizzardActive = true;
        blizzardTicks = BLIZZARD_DURATION;
        blizzardCenter = getLocation();

        blizzardCenter.getWorld().playSound(blizzardCenter, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
        blizzardCenter.getWorld().playSound(blizzardCenter, Sound.ITEM_TRIDENT_RETURN, 1.3f, 0.45f);
        blizzardCenter.getWorld().playSound(blizzardCenter, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.1f, 0.4f);

        TrailCircleHelper.spawnRadialBurstRing(
                blizzardCenter.clone().add(0, 3, 0), BLIZZARD_RADIUS, 2.0,
                ICE_WHITE, 40, 48, new Vector(0, 1, 0), 0);

        TrailCircleHelper.spawnCircle(blizzardCenter.clone().add(0, 0.1, 0), BLIZZARD_RADIUS, ICE_PALE, 40, 48);
        TrailCircleHelper.spawnExpandingShockwave(
                plugin, blizzardCenter.clone().add(0, 0.15, 0),
                2.0, BLIZZARD_RADIUS, ICE_DARK, 18, 36
        );

        blizzardCooldown = currentPhase == 3 ? P3_BLIZZARD_COOLDOWN : BLIZZARD_COOLDOWN;
    }

    private void tickBlizzard() {
        if (blizzardTicks <= 0) {
            Location endCenter = blizzardCenter;
            if (endCenter != null && endCenter.getWorld() != null) {
                endCenter.getWorld().playSound(endCenter, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.45f);
                TrailCircleHelper.spawnExpandingShockwave(
                        plugin, endCenter.clone().add(0, 0.15, 0),
                        BLIZZARD_RADIUS, 1.0, ICE_WHITE, 16, 36
                );
            }
            blizzardActive = false;
            blizzardCenter = null;
            return;
        }
        blizzardTicks--;

        if (blizzardTicks % 10 != 0) return;

        World world = blizzardCenter.getWorld();
        for (int i = 0; i < 14; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * BLIZZARD_RADIUS;
            double x = blizzardCenter.getX() + r * Math.cos(angle);
            double z = blizzardCenter.getZ() + r * Math.sin(angle);
            double y = blizzardCenter.getY() + 5 + RANDOM.nextDouble() * 5;
            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0.3, 0.3, 0.3, 0.05);
        }

        for (int i = 0; i < 4; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * BLIZZARD_RADIUS;
            Location ground = blizzardCenter.clone().add(
                    Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius
            );
            Location sky = ground.clone().add(0, 7.0 + RANDOM.nextDouble() * 4.0, 0);
            TrailHelper.spawnLine(sky, ground, ICE_PALE, 7);
        }

        if (blizzardTicks % 40 == 0) {
            world.playSound(blizzardCenter, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.45f);
            world.playSound(blizzardCenter, Sound.ENTITY_STRAY_AMBIENT, 0.7f, 0.35f);
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

    private void updateBossBar() {
        if (bossBar == null || entity == null || !entity.isValid()) {
            return;
        }

        double maxHealth = getMaxHealth();
        double progress = maxHealth <= 0.0 ? 0.0 : getHealth() / maxHealth;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        bossBar.setTitle(bossBarTitle());

        boolean showStagger = updateStaggerBar();
        Location bossLocation = getLocation();
        double visibleRangeSquared = 56.0 * 56.0;
        for (Player viewer : List.copyOf(bossBar.getPlayers())) {
            if (!canViewBossBars(viewer, bossLocation, visibleRangeSquared)) {
                bossBar.removePlayer(viewer);
            }
        }
        if (staggerBar != null) {
            for (Player viewer : List.copyOf(staggerBar.getPlayers())) {
                if (!showStagger || !canViewBossBars(viewer, bossLocation, visibleRangeSquared)) {
                    staggerBar.removePlayer(viewer);
                }
            }
        }

        for (Player viewer : entity.getWorld().getPlayers()) {
            if (!canViewBossBars(viewer, bossLocation, visibleRangeSquared)) {
                continue;
            }
            if (!bossBar.getPlayers().contains(viewer)) {
                bossBar.addPlayer(viewer);
            }
            if (showStagger && staggerBar != null && !staggerBar.getPlayers().contains(viewer)) {
                staggerBar.addPlayer(viewer);
            }
        }
    }

    private boolean updateStaggerBar() {
        if (staggerBar == null) {
            return false;
        }

        boolean visible = introPhase == IntroPhase.ACTIVE
                && !phaseTransitioning
                && (staggerState.getCurrent() > 0.0 || staggerState.isStaggered());
        staggerBar.setVisible(visible);
        if (!visible) {
            return false;
        }

        if (staggerState.isStaggered()) {
            double remaining = staggerState.getStaggerTicksRemaining()
                    / (double) STAGGER_PROFILE.staggerDurationTicks();
            staggerBar.setProgress(Math.max(0.0, Math.min(1.0, remaining)));
            staggerBar.setColor(BarColor.WHITE);
            staggerBar.setTitle(String.format(
                    "§f§l✦ 体勢崩壊 §7— §c%.1f秒",
                    staggerState.getStaggerTicksRemaining() / 20.0
            ));
            return true;
        }

        double progress = staggerState.getCurrent() / STAGGER_PROFILE.maxStagger();
        staggerBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        staggerBar.setColor(progress >= 0.8 ? BarColor.RED : BarColor.YELLOW);
        staggerBar.setTitle(progress >= 0.8 ? "§c§l体勢 §7— §f崩壊寸前" : "§e§l体勢");
        return true;
    }

    private boolean canViewBossBars(Player viewer, Location bossLocation, double visibleRangeSquared) {
        return viewer.isOnline()
                && !viewer.isDead()
                && viewer.getWorld().equals(entity.getWorld())
                && viewer.getLocation().distanceSquared(bossLocation) <= visibleRangeSquared;
    }

    private String bossBarTitle() {
        return switch (currentPhase) {
            case 2 -> "§9§l氷結の巡礼者 §7— §b凍界";
            case 3 -> "§f§l氷結の巡礼者 §7— §c終氷";
            default -> "§b§l氷結の巡礼者 §7— §f静謐";
        };
    }

    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
        if (staggerBar != null) {
            staggerBar.removeAll();
            staggerBar.setVisible(false);
            staggerBar = null;
        }
    }

    private boolean isDead() {
        return entity == null || entity.isDead() || !entity.isValid();
    }
}
