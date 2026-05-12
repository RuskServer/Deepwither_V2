package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
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
        return List.of("雷球を放ち、命中した敵から", "周囲の敵へ連鎖する。");
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
    public double getManaCost(SkillContext context) { return 30.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(4); }

    @Override
    public CastResult cast(SkillContext context) {
        SkillProjectile projectile = new SkillProjectile(
                context.getCaster(),
                context.getEyeLocation().add(context.getDirection().multiply(0.6)),
                context.getDirection(),
                1.4,
                0.8,
                60
        ) {
            private int chainsRemaining = 3;

            @Override
            protected void onTick() {
                getCurrentLocation().getWorld().spawnParticle(Particle.ELECTRIC_SPARK, getCurrentLocation(), 2, 0.1, 0.1, 0.1, 0.02);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                chain(target, getCurrentLocation());
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                remove();
            }

            private void chain(LivingEntity hit, Location origin) {
                double damage = 25.0 + (context.getLevel() * 5.0);
                damagePipelineManager.processDamage(context.getCaster(), hit, DamageType.MAGIC, damage, getTags());

                origin.getWorld().spawnParticle(Particle.FLASH, hit.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                origin.getWorld().playSound(hit.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.5f);

                if (chainsRemaining <= 0) return;
                chainsRemaining--;

                LivingEntity next = null;
                double nearest = 5.0;
                for (LivingEntity entity : hit.getLocation().getNearbyLivingEntities(5.0)) {
                    if (entity.equals(context.getCaster()) || entity.equals(hit)) continue;
                    double dist = entity.getLocation().distance(hit.getLocation());
                    if (dist < nearest) {
                        nearest = dist;
                        next = entity;
                    }
                }

                if (next != null) {
                    spawnChainParticles(hit.getLocation().add(0, 1, 0), next.getLocation().add(0, 1, 0));
                    chain(next, next.getLocation());
                }
            }

            private void spawnChainParticles(Location from, Location to) {
                Vector direction = to.toVector().subtract(from.toVector());
                double length = direction.length();
                direction.normalize();
                for (double d = 0; d < length; d += 0.5) {
                    Location point = from.clone().add(direction.clone().multiply(d));
                    from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);
                }
            }
        };

        if (projectileService.launch(projectile)) {
            context.getCaster().playSound(context.getCaster().getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2.0f);
            return CastResult.success();
        }
        return CastResult.fail();
    }
}
