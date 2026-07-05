package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class ThunderStrikeSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;
    private final JavaPlugin plugin;

    @Inject
    public ThunderStrikeSkill(DamagePipelineManager damagePipelineManager, JavaPlugin plugin) {
        this.damagePipelineManager = damagePipelineManager;
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "thunder_strike"; }

    @Override
    public String getDisplayName() { return "サンダーストライク"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "指定地点に予兆を描き、1秒後に雷を落とす。",
                "周囲5mの敵に魔法ダメージ(500%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.LOCATION; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "lightning", "area"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.CHANNELING); }

    @Override
    public double getManaCost(SkillContext context) { return 40.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(15); }

    @Override
    public Duration getCastTime(SkillContext context) { return Duration.ofMillis(300); }

    @Override
    public CastResult cast(SkillContext context) {
        Location targetLoc = context.getTargetLocation();
        if (targetLoc == null) {
            targetLoc = context.getCaster().getTargetBlock(null, 20).getLocation();
        }

        final Location strikeLoc = targetLoc.clone().add(0.5, 0.1, 0.5);
        final double radius = 5.0;
        final LivingEntity caster = context.getCaster();

        strikeLoc.getWorld().playSound(strikeLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 2.0f);
        strikeLoc.getWorld().playSound(strikeLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 20) {
                    strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
                    for (int i = 0; i < 3; i++) {
                        double ox = (Math.random() - 0.5) * 2.0;
                        double oz = (Math.random() - 0.5) * 2.0;
                        strikeLoc.getWorld().strikeLightningEffect(strikeLoc.clone().add(ox, 0, oz));
                    }

                    strikeLoc.getWorld().spawnParticle(Particle.FLASH, strikeLoc, 5, 1.0, 1.0, 1.0, 0, Color.WHITE);
                    strikeLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, strikeLoc, 3, 2.0, 0.5, 2.0, 0);
                    strikeLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, strikeLoc, 1, 0, 0, 0, 0);

                    // 衝撃波リング
                    TrailCircleHelper.spawnCircle(strikeLoc, radius, Color.fromRGB(100, 149, 237), 15, 36);
                    TrailCircleHelper.spawnCircle(strikeLoc, radius - 1.0, Color.fromRGB(70, 130, 255), 12, 30, new Vector(0, 1, 0), 30);

                    for (int i = 0; i < 50; i++) {
                        double angle = Math.toRadians(i * (360.0 / 50.0));
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location particleLoc = strikeLoc.clone().add(x, 0.5, z);
                        strikeLoc.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0.1, 0, 0.1);
                    }

                    strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
                    strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

                    strikeLoc.getWorld().getNearbyEntities(strikeLoc, radius, 4.0, radius).forEach(entity -> {
                        if (entity instanceof LivingEntity victim && !entity.equals(caster)) {
                            damagePipelineManager.processScaledDamage(caster, victim, DamageType.MAGIC, 5.0, getTags());
                        }
                    });

                    cancel();
                    return;
                }

                // 地面の予兆ブロックリング
                if (tick % 4 == 0) {
                    for (int i = 0; i < 36; i++) {
                        double angle = Math.toRadians(i * 10);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        strikeLoc.getWorld().spawnParticle(Particle.BLOCK, strikeLoc.clone().add(x, -0.05, z), 1, 0, 0, 0, 0, Material.GLOWSTONE.createBlockData());
                        strikeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc.clone().add(x, 0.8, z), 2, 0, 0.1, 0, 0.05);
                    }
                }

                // 上から降りてくるリング（徐々に大きく・低くなる）
                double progress = (double) tick / 20.0;
                double height = 15.0 * (1.0 - progress);
                double ringRadius = radius * (0.2 + 0.8 * progress);
                TrailCircleHelper.spawnCircle(strikeLoc.clone().add(0, height, 0), ringRadius, Color.fromRGB(100, 149, 237), 8, 16 + tick);

                // FLASH 蓄積（0.5秒前から）
                if (tick >= 10) {
                    float intensity = (float) (tick - 10) / 10.0f;
                    strikeLoc.getWorld().spawnParticle(Particle.FLASH, strikeLoc, 1 + (int) (intensity * 3), 0, 0, 0, 0, Color.WHITE);
                    strikeLoc.getWorld().spawnParticle(Particle.GLOW, strikeLoc, (int) (5 + intensity * 25), radius * 0.4, 0.5, radius * 0.4, 0.02);
                    strikeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc.clone().add(0, height + 2, 0), 6 + (int) (intensity * 10), 1.0, 0.5, 1.0, 0.05);
                }

                strikeLoc.getWorld().spawnParticle(Particle.ENCHANT, strikeLoc, 5, 1.5, 0.3, 1.5, 0.03);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return CastResult.success();
    }
}
