package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class IceSpikeSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public IceSpikeSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "ice_spike"; }

    @Override
    public String getDisplayName() { return "アイススパイク"; }

    @Override
    public List<String> getDescription() {
        return List.of("標的の足元から氷の棘を噴出させ、範囲内の敵に大ダメージを与える。");
    }

    @Override
    public Material getIcon() { return Material.PACKED_ICE; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.LOCATION; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "ice", "area"); }

    @Override
    public double getManaCost(SkillContext context) { return 45.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(8); }

    @Override
    public Duration getCastTime(SkillContext context) { return Duration.ofMillis(1000); }

    @Override
    public CastResult cast(SkillContext context) {
        Location targetLoc = context.getTargetLocation();
        if (targetLoc == null) {
            targetLoc = context.getCaster().getTargetBlock(null, 10).getLocation();
        }

        Location finalLoc = targetLoc.clone();

        for (int i = 0; i < 5; i++) {
            double height = i * 1.0;
            context.getCaster().getWorld().spawnParticle(Particle.SNOWFLAKE, finalLoc.clone().add(0, height, 0), 20, 0.3, 0.5, 0.3, 0.05);
            context.getCaster().getWorld().spawnParticle(Particle.POOF, finalLoc.clone().add(0, height, 0), 5, 0.2, 0.2, 0.2, 0.0);
        }

        finalLoc.getWorld().playSound(finalLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);

        double damage = 60.0 + (context.getLevel() * 12.0);
        finalLoc.getWorld().getNearbyEntities(finalLoc, 2.5, 5.0, 2.5).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                damagePipelineManager.processDamage(context.getCaster(), living, DamageType.MAGIC, damage, getTags());
            }
        });

        return CastResult.success();
    }
}
