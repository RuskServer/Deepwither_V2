package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class FireNovaSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public FireNovaSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() {
        return "fire_nova";
    }

    @Override
    public String getDisplayName() {
        return "ファイアノヴァ";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "自身の周囲に炎の衝撃波を発生させ、敵を吹き飛ばす。",
                "周囲5mの敵に魔法ダメージ(400%)を与える。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.FIREWORK_STAR;
    }

    @Override
    public SkillCategory getCategory() {
        return SkillCategory.ACTIVE;
    }

    @Override
    public SkillTargetType getTargetType() {
        return SkillTargetType.SELF;
    }

    @Override
    public Set<String> getTags() {
        return Set.of("magic", "fire", "area");
    }

    @Override
    public Set<SkillTag.Role> getRoles() {
        return Set.of(SkillTag.Role.ATTACK, SkillTag.Role.CONTROL);
    }

    @Override
    public Set<SkillTag.Tactic> getTactics() {
        return Set.of(SkillTag.Tactic.BURST, SkillTag.Tactic.DISPLACE);
    }

    @Override
    public Set<SkillTag.Scaling> getScalings() {
        return Set.of(SkillTag.Scaling.MAGICAL);
    }

    @Override
    public Set<SkillTag.Constraint> getConstraints() {
        return Set.of(SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD);
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 60.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(12);
    }

    @Override
    public CastResult cast(SkillContext context) {
        context.getCaster().getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, context.getCaster().getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        context.getCaster().getWorld().spawnParticle(Particle.FLAME, context.getCaster().getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.2);
        context.getCaster().getWorld().playSound(context.getCaster().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        
        double range = 5.0;

        context.getCaster().getNearbyEntities(range, range, range).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                damagePipelineManager.processScaledDamage(context.getCaster(), living, DamageType.MAGIC, 4.0, getTags());
                living.setVelocity(living.getLocation().toVector().subtract(context.getCaster().getLocation().toVector()).normalize().multiply(1.5).setY(0.5));
            }
        });

        return CastResult.success();
    }
}
