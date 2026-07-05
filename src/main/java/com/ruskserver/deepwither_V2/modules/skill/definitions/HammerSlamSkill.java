package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class HammerSlamSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public HammerSlamSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "hammer_slam"; }

    @Override
    public String getDisplayName() { return "ハンマースラム"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "地面を叩きつけ、周囲の敵を吹き飛ばす。",
                "周囲3.5mの敵に物理ダメージ(150%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.ANVIL; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "heavy"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK, SkillTag.Role.CONTROL); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.DISPLACE); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 25.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(6); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var loc = player.getLocation();
        var world = loc.getWorld();

        world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 0.5f);

        // 広がる衝撃波リング
        TrailCircleHelper.spawnCircle(loc, 1.0, Color.fromRGB(160, 120, 80), 8, 12);
        TrailCircleHelper.spawnCircle(loc, 2.0, Color.fromRGB(140, 100, 60), 7, 16, new Vector(0, 1, 0), 30);
        TrailCircleHelper.spawnCircle(loc, 3.5, Color.fromRGB(120, 80, 40), 6, 20);

        world.spawnParticle(Particle.BLOCK, loc, 40, 3.5, 0.2, 3.5, 0.3, Material.STONE.createBlockData());
        world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.5, 0), 20, 3.0, 0.5, 3.0, 0.2, Material.DIRT.createBlockData());
        world.spawnParticle(Particle.CRIT, loc.clone().add(0, 0.3, 0), 20, 3.0, 0.3, 3.0, 0.15);
        world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.7f);

        player.getNearbyEntities(3.5, 3.5, 3.5).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                damagePipelineManager.processScaledDamage(player, living, DamageType.PHYSICAL, 1.5, getTags());
                living.setVelocity(living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2).setY(0.4));
            }
        });

        return CastResult.success();
    }
}
