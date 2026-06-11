package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class MassHealSkill implements Skill {

    private final VirtualHealthManager healthManager;

    @Inject
    public MassHealSkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "mass_heal"; }

    @Override
    public String getDisplayName() { return "大聖光"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "両手を掲げ聖なる波動を放ち、周囲の味方を一斉に癒す。",
                "周囲10mの味方に最大HPの30%を回復する。"
        );
    }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "heal", "support", "aoe"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 60.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(30); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();
        var loc = caster.getLocation().add(0, 1, 0);

        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 60, 5.0, 1.0, 5.0, 0.1);
        loc.getWorld().spawnParticle(Particle.HEART, loc, 30, 5.0, 1.0, 5.0, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        caster.getNearbyEntities(10.0, 10.0, 10.0).forEach(entity -> {
            if (entity instanceof Player ally && !entity.equals(caster)) {
                double maxHp = healthManager.getMaxHealth(ally);
                healthManager.heal(ally, maxHp * 0.3);
                var allyLoc = ally.getLocation().add(0, 1, 0);
                allyLoc.getWorld().spawnParticle(Particle.HEART, allyLoc, 8, 0.4, 0.4, 0.4, 0);
            }
        });

        double selfMaxHp = healthManager.getMaxHealth(caster);
        healthManager.heal(caster, selfMaxHp * 0.3);

        return CastResult.success();
    }
}
