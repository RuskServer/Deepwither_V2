package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class IceShardSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public IceShardSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "ice_shard"; }

    @Override
    public String getDisplayName() { return "アイスシャード"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方に氷の欠片を放ち、着弾地点で砕け散る。",
                "周囲3mの敵に魔法ダメージ(150%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.ICE; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.PROJECTILE; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "ice", "projectile"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL, SkillTag.Scaling.CDR_HEAVY); }

    @Override
    public double getManaCost(SkillContext context) { return 20.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(2); }

    @Override
    public CastResult cast(SkillContext context) {
        SkillProjectile projectile = new SkillProjectile(
                context.getCaster(),
                context.getEyeLocation().add(context.getDirection().multiply(0.6)),
                context.getDirection(),
                1.2,
                0.8,
                80
        ) {
            @Override
            protected void onTick() {
                getCurrentLocation().getWorld().spawnParticle(Particle.SNOWFLAKE, getCurrentLocation(), 3, 0.1, 0.1, 0.1, 0.02);
                getCurrentLocation().getWorld().spawnParticle(Particle.POOF, getCurrentLocation(), 1, 0.05, 0.05, 0.05, 0.01);
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
                getCurrentLocation().getWorld().spawnParticle(Particle.POOF, getCurrentLocation(), 15, 0.4, 0.4, 0.4, 0.1);
                getCurrentLocation().getWorld().spawnParticle(Particle.SNOWFLAKE, getCurrentLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                getCurrentLocation().getWorld().playSound(getCurrentLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);

                getCurrentLocation().getWorld().getNearbyEntities(getCurrentLocation(), 3.0, 3.0, 3.0).forEach(entity -> {
                    if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                        damagePipelineManager.processScaledDamage(context.getCaster(), living, DamageType.MAGIC, 1.5, getTags());
                    }
                });
            }
        };

        if (projectileService.launch(projectile)) {
            context.getCaster().playSound(context.getCaster().getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.8f, 1.2f);
            return CastResult.success();
        }
        return CastResult.fail();
    }
}
