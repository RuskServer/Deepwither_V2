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
public class ThunderStrikeSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public ThunderStrikeSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "thunder_strike"; }

    @Override
    public String getDisplayName() { return "サンダーストライク"; }

    @Override
    public List<String> getDescription() {
        return List.of("対象地点に雷を落とし、", "範囲内の敵にダメージを与える。");
    }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.LOCATION; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "lightning", "area"); }

    @Override
    public double getManaCost(SkillContext context) { return 40.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(7); }

    @Override
    public Duration getCastTime(SkillContext context) { return Duration.ofMillis(800); }

    @Override
    public CastResult cast(SkillContext context) {
        Location targetLoc = context.getTargetLocation();
        if (targetLoc == null) {
            targetLoc = context.getCaster().getTargetBlock(null, 15).getLocation();
        }

        Location strikeLoc = targetLoc.clone().add(0, 1, 0);
        strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
        strikeLoc.getWorld().spawnParticle(Particle.FLASH, strikeLoc, 1, 0, 0, 0, 0);
        strikeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc, 30, 0.4, 0.4, 0.4, 0.1);
        strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);

        double damage = 50.0 + (context.getLevel() * 10.0);
        strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3.0, 3.0, 3.0).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                damagePipelineManager.processDamage(context.getCaster(), living, DamageType.MAGIC, damage, getTags());
            }
        });

        return CastResult.success();
    }
}
