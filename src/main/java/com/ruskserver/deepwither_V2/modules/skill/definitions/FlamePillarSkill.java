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
public class FlamePillarSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public FlamePillarSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() {
        return "flame_pillar";
    }

    @Override
    public String getDisplayName() {
        return "フレイムピラー";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "指定地点の足元から巨大な火柱を噴出させる。",
                "周囲2.5mの敵に魔法ダメージ(300%)を与える。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.BLAZE_ROD;
    }

    @Override
    public SkillCategory getCategory() {
        return SkillCategory.ACTIVE;
    }

    @Override
    public SkillTargetType getTargetType() {
        return SkillTargetType.LOCATION;
    }

    @Override
    public Set<String> getTags() {
        return Set.of("magic", "fire", "area");
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 45.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(8);
    }

    @Override
    public Duration getCastTime(SkillContext context) {
        return Duration.ofMillis(1000);
    }

    @Override
    public CastResult cast(SkillContext context) {
        Location targetLoc = context.getTargetLocation();
        if (targetLoc == null) {
            // ターゲットがない場合は前方の地面を探すなどの処理が必要だが、
            // 基礎実装では単純に失敗とするか、前方の一定距離とする
            targetLoc = context.getCaster().getTargetBlock(null, 10).getLocation();
        }

        Location finalLoc = targetLoc.clone();
        
        // 火柱の演出とダメージ
        for (int i = 0; i < 5; i++) {
            double height = i * 1.0;
            context.getCaster().getWorld().spawnParticle(Particle.FLAME, finalLoc.clone().add(0, height, 0), 30, 0.3, 0.5, 0.3, 0.05);
            context.getCaster().getWorld().spawnParticle(Particle.LAVA, finalLoc.clone().add(0, height, 0), 5, 0.2, 0.2, 0.2, 0.0);
        }
        
        finalLoc.getWorld().playSound(finalLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 0.8f);
        
        finalLoc.getWorld().getNearbyEntities(finalLoc, 2.5, 5.0, 2.5).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                damagePipelineManager.processScaledDamage(context.getCaster(), living, DamageType.MAGIC, 3.0, getTags());
            }
        });

        return CastResult.success();
    }
}
