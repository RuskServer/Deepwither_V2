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
public class TauntSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public TauntSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "taunt"; }

    @Override
    public String getDisplayName() { return "挑発"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "周囲の敵を挑発し、怒気で押し返す。",
                "周囲6mの敵に魔法ダメージ(75%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.IRON_CHESTPLATE; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "defense"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.DEFENSE, SkillTag.Role.CONTROL); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.DISPLACE); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 15.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(6); }

    @Override
    public CastResult cast(SkillContext context) {
        var loc = context.getCaster().getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, loc, 15, 1.5, 0.5, 1.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.8f);

        context.getCaster().getNearbyEntities(6.0, 6.0, 6.0).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                damagePipelineManager.processScaledDamage(context.getCaster(), living, DamageType.MAGIC, 0.75, getTags());
                living.setVelocity(living.getLocation().toVector().subtract(context.getCaster().getLocation().toVector()).normalize().multiply(0.3).setY(0.2));
            }
        });

        return CastResult.success();
    }
}
