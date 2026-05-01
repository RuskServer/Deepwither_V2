package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillProjectile;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
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
public class ArcaneBoltSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public ArcaneBoltSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() {
        return "arcane_bolt";
    }

    @Override
    public String getDisplayName() {
        return "アーケインボルト";
    }

    @Override
    public List<String> getDescription() {
        return List.of("前方に魔力弾を放ち、命中した敵に魔法ダメージを与える。");
    }

    @Override
    public Material getIcon() {
        return Material.AMETHYST_SHARD;
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
        return Set.of("magic", "projectile");
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 18.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(3);
    }

    @Override
    public CastResult cast(SkillContext context) {
        SkillProjectile projectile = new SkillProjectile(
                context.getCaster(),
                context.getEyeLocation().add(context.getDirection().multiply(0.6)),
                context.getDirection(),
                1.0,
                0.6,
                60
        ) {
            @Override
            protected void onTick() {
                getCurrentLocation().getWorld().spawnParticle(Particle.ENCHANT, getCurrentLocation(), 8, 0.08, 0.08, 0.08, 0.0);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                damagePipelineManager.processDamage(context.getCaster(), target, DamageType.MAGIC, 24.0 + (context.getLevel() * 4.0), getTags());
                target.getWorld().spawnParticle(Particle.CRIT, getCurrentLocation(), 12, 0.2, 0.2, 0.2, 0.1);
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                block.getWorld().spawnParticle(Particle.SMOKE, getCurrentLocation(), 8, 0.15, 0.15, 0.15, 0.0);
                remove();
            }
        };

        boolean launched = projectileService.launch(projectile);
        if (launched) {
            context.getCaster().playSound(context.getCaster().getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.5f);
            return CastResult.success();
        }
        return CastResult.fail();
    }
}
