package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class FirstAidSkill implements Skill {

    private final VirtualHealthManager healthManager;

    @Inject
    public FirstAidSkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() {
        return "first_aid";
    }

    @Override
    public String getDisplayName() {
        return "応急手当";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "1.2秒の詠唱後、応急処置で自身を立て直す。",
                "自身の最大HPを25%回復する。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.HONEY_BOTTLE;
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
        return Set.of("heal", "support");
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 25.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(10);
    }

    @Override
    public Duration getCastTime(SkillContext context) {
        return Duration.ofMillis(1200);
    }

    @Override
    public CastResult cast(SkillContext context) {
        double maxHealth = healthManager.getMaxHealth(context.getCaster());
        healthManager.heal(context.getCaster(), maxHealth * 0.25);
        context.getCaster().getWorld().spawnParticle(Particle.HEART, context.getCaster().getLocation().add(0, 1.2, 0), 6, 0.4, 0.5, 0.4, 0.0);
        context.getCaster().playSound(context.getCaster().getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
        return CastResult.success();
    }
}
