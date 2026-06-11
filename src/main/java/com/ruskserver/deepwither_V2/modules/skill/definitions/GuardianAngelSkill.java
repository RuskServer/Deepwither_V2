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
public class GuardianAngelSkill implements Skill {

    private final VirtualHealthManager healthManager;

    @Inject
    public GuardianAngelSkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "guardian_angel"; }

    @Override
    public String getDisplayName() { return "守護天使"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "天より聖なる守護者を召喚し、味方全体を守る障壁で包む。",
                "周囲10mの味方全員に最大HPの40%のダメージ吸収バリア(10秒)を付与する。"
        );
    }

    @Override
    public Material getIcon() { return Material.TOTEM_OF_UNDYING; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "defense", "barrier", "support", "aoe"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.DEFENSE, SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 50.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(60); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();
        var loc = caster.getLocation().add(0, 1, 0);

        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 50, 5.0, 1.0, 5.0, 0.15);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

        double selfMaxHp = healthManager.getMaxHealth(caster);
        healthManager.setBarrier(caster, selfMaxHp * 0.4);

        caster.getNearbyEntities(10.0, 10.0, 10.0).forEach(entity -> {
            if (entity instanceof Player ally && !entity.equals(caster)) {
                double maxHp = healthManager.getMaxHealth(ally);
                healthManager.setBarrier(ally, maxHp * 0.4);
                var allyLoc = ally.getLocation().add(0, 1, 0);
                allyLoc.getWorld().spawnParticle(Particle.END_ROD, allyLoc, 15, 0.5, 0.5, 0.5, 0.05);
                allyLoc.getWorld().playSound(allyLoc, Sound.ITEM_SHIELD_BLOCK, 0.6f, 1.5f);
            }
        });

        return CastResult.success();
    }
}
