package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailHelper;
import org.bukkit.Color;
import org.bukkit.Location;
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
public class ChainLightningSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public ChainLightningSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "chain_lightning"; }

    @Override
    public String getDisplayName() { return "チェインライトニング"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方に雷球を放ち、命中した敵から近くの敵へ連鎖する。",
                "最大4体に魔法ダメージ(125%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.FIREWORK_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.PROJECTILE; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "lightning", "projectile", "chain"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 30.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(4); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var eyeLoc = context.getEyeLocation();
        var dir = context.getDirection().clone();

        // 発射エフェクト
        TrailCircleHelper.spawnCircle(eyeLoc, 0.4, Color.fromRGB(100, 149, 237), 6, 8, dir, 0);
        TrailCircleHelper.spawnCircle(eyeLoc, 0.6, Color.fromRGB(70, 130, 255), 5, 10, dir, 45);
        eyeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, eyeLoc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.2);

        SkillProjectile projectile = new SkillProjectile(
                player,
                eyeLoc.add(dir.clone().multiply(0.6)),
                dir,
                1.4,
                0.8,
                60
        ) {
            private int chainsRemaining = 3;

            @Override
            protected void onTick() {
                var loc = getCurrentLocation();
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 2, 0.1, 0.1, 0.1, 0.02);
                loc.getWorld().spawnParticle(Particle.GLOW, loc, 1, 0.05, 0.05, 0.05, 0);
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 0, dir.getX(), dir.getY(), dir.getZ(), 0.08);
                TrailCircleHelper.spawnCircle(loc, 0.2, Color.fromRGB(70, 130, 255), 3, 4, dir, getTicksLived() * 45);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                chain(target, getCurrentLocation(), player);
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                remove();
            }

            private void chain(LivingEntity hit, Location origin, LivingEntity caster) {
                damagePipelineManager.processScaledDamage(caster, hit, DamageType.MAGIC, 1.25, getTags(), getId(), 500L);

                var hitLoc = hit.getLocation().add(0, 1, 0);
                origin.getWorld().spawnParticle(Particle.FLASH, hitLoc, 1, 0, 0, 0, 0, Color.WHITE);
                origin.getWorld().spawnParticle(Particle.SONIC_BOOM, hitLoc, 3, 0.5, 0.5, 0.5, 0);
                origin.getWorld().playSound(hit.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.5f);

                TrailCircleHelper.spawnCircle(hit.getLocation().add(0, 0.1, 0), 1.0, Color.fromRGB(70, 130, 255), 6, 12);

                if (chainsRemaining <= 0) return;
                chainsRemaining--;

                LivingEntity next = null;
                double nearest = 5.0;
                for (LivingEntity entity : hit.getLocation().getNearbyLivingEntities(5.0)) {
                    if (entity.equals(caster) || entity.equals(hit)) continue;
                    double dist = entity.getLocation().distance(hit.getLocation());
                    if (dist < nearest) {
                        nearest = dist;
                        next = entity;
                    }
                }

                if (next != null) {
                    var nextLoc = next.getLocation().add(0, 1, 0);
                    TrailHelper.spawnLine(hitLoc, nextLoc, Color.fromRGB(70, 130, 255), 4);
                    chain(next, next.getLocation(), caster);
                }
            }
        };

        if (projectileService.launch(projectile)) {
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2.0f);
            return CastResult.success();
        }
        return CastResult.fail();
    }
}
