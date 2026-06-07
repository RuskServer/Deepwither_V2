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
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class FlameBreathSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public FlameBreathSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() {
        return "flame_breath";
    }

    @Override
    public String getDisplayName() {
        return "フレイムブレス";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方へ炎を吹き付け、視線方向の敵を焼き払う。",
                "最大6m先の敵に魔法ダメージ(125%)を与える。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.MAGMA_CREAM;
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
        return Set.of("magic", "fire", "breath");
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 35.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(5);
    }

    @Override
    public CastResult cast(SkillContext context) {
        Location eyeLoc = context.getEyeLocation();
        Vector direction = context.getDirection();
        
        // 扇状に炎を出す演出
        for (int i = 0; i < 20; i++) {
            Vector spread = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).multiply(0.2);
            Vector v = direction.clone().add(spread).multiply(0.5 + Math.random() * 0.5);
            eyeLoc.getWorld().spawnParticle(Particle.FLAME, eyeLoc, 0, v.getX(), v.getY(), v.getZ(), 0.1);
        }
        
        eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f);
        
        double range = 6.0;
        double angleCos = Math.cos(Math.toRadians(45)); // 45度の扇形

        eyeLoc.getWorld().getNearbyEntities(eyeLoc, range, range, range).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                Vector toTarget = living.getLocation().toVector().subtract(eyeLoc.toVector());
                if (toTarget.length() <= range) {
                    double dot = toTarget.normalize().dot(direction);
                    if (dot >= angleCos) {
                        damagePipelineManager.processScaledDamage(context.getCaster(), living, DamageType.MAGIC, 1.25, getTags());
                        living.getWorld().spawnParticle(Particle.FLAME, living.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        });

        return CastResult.success();
    }
}
