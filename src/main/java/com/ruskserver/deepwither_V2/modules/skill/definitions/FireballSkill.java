package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class FireballSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public FireballSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() {
        return "fireball";
    }

    @Override
    public String getDisplayName() {
        return "ファイアボール";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方に火球を放ち、着弾地点で爆発する。",
                "周囲3mの敵に魔法ダメージ(150%)を与える。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.FIRE_CHARGE;
    }

    @Override
    public SkillCategory getCategory() {
        return SkillCategory.ACTIVE;
    }

    @Override
    public SkillTargetType getTargetType() {
        return SkillTargetType.PROJECTILE;
    }

    @Override
    public Set<String> getTags() {
        return Set.of("magic", "fire", "projectile");
    }

    @Override
    public Set<SkillTag.Role> getRoles() {
        return Set.of(SkillTag.Role.ATTACK);
    }

    @Override
    public Set<SkillTag.Tactic> getTactics() {
        return Set.of(SkillTag.Tactic.BURST);
    }

    @Override
    public Set<SkillTag.Scaling> getScalings() {
        return Set.of(SkillTag.Scaling.MAGICAL, SkillTag.Scaling.CDR_HEAVY);
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 20.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(2);
    }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var eyeLoc = context.getEyeLocation();
        var dir = context.getDirection().clone();

        TrailCircleHelper.spawnCircle(eyeLoc, 0.5, Color.fromRGB(255, 100, 0), 6, 8, dir, 0);
        TrailCircleHelper.spawnCircle(eyeLoc, 0.3, Color.fromRGB(255, 200, 50), 5, 6, dir, 45);
        eyeLoc.getWorld().spawnParticle(Particle.FLAME, eyeLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.2);

        SkillProjectile projectile = new SkillProjectile(
                player,
                eyeLoc.add(dir.clone().multiply(0.6)),
                dir,
                1.2,
                0.8,
                80
        ) {
            @Override
            protected void onTick() {
                var loc = getCurrentLocation();
                var world = loc.getWorld();
                world.spawnParticle(Particle.FLAME, loc, 3, 0.15, 0.15, 0.15, 0.02);
                world.spawnParticle(Particle.SMOKE, loc, 1, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.GLOW, loc, 2, 0.1, 0.1, 0.1, 0);
                world.spawnParticle(Particle.FLAME, loc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.05);
                TrailCircleHelper.spawnCircle(loc, 0.4, Color.fromRGB(255, 150, 50), 4, 6, dir, getTicksLived() * 30);
                TrailCircleHelper.spawnCircle(loc, 0.6, Color.fromRGB(255, 100, 0), 4, 8, dir, getTicksLived() * 30 + 60);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                explode();
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                explode();
                remove();
            }

            private void explode() {
                var loc = getCurrentLocation();
                var world = loc.getWorld();
                world.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.FLAME, loc, 30, 1.5, 1.5, 1.5, 0.15);
                world.spawnParticle(Particle.GLOW, loc, 20, 1.0, 1.0, 1.0, 0.05);
                world.spawnParticle(Particle.LAVA, loc, 5, 0.5, 0.5, 0.5, 0);
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                TrailCircleHelper.spawnCircle(loc, 1.0, Color.fromRGB(255, 150, 50), 8, 12);
                TrailCircleHelper.spawnCircle(loc, 2.0, Color.fromRGB(255, 100, 0), 6, 16, new Vector(0, 1, 0), 30);
                TrailCircleHelper.spawnCircle(loc, 3.0, Color.fromRGB(200, 50, 0), 5, 20);

                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    double x = Math.cos(angle) * 0.8;
                    double z = Math.sin(angle) * 0.8;
                    world.spawnParticle(Particle.FLAME, loc, 0, x, 0.3, z, 0.15);
                }

                loc.getWorld().getNearbyEntities(loc, 3.0, 3.0, 3.0).forEach(entity -> {
                    if (entity instanceof LivingEntity living && !entity.equals(player)) {
                        damagePipelineManager.processScaledDamage(player, living, DamageType.MAGIC, 1.5, getTags());
                    }
                });
            }
        };

        if (projectileService.launch(projectile)) {
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
            return CastResult.success();
        }
        return CastResult.fail();
    }
}
