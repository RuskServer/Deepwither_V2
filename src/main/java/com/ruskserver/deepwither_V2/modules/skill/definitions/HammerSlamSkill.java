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
        return List.of("地面を叩きつけ、周囲の敵にダメージとノックバックを与える。");
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
    public double getManaCost(SkillContext context) { return 25.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(6); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var loc = player.getLocation();

        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 30, 1.0, 0.2, 1.0, 0.2, org.bukkit.Material.STONE.createBlockData());
        loc.getWorld().spawnParticle(Particle.CRIT, loc.add(0, 0.5, 0), 15, 1.0, 0.1, 1.0, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.7f);

        double damage = 30.0 + (context.getLevel() * 6.0);
        player.getNearbyEntities(3.5, 3.5, 3.5).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                damagePipelineManager.processDamage(player, living, DamageType.PHYSICAL, damage, getTags());
                living.setVelocity(living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2).setY(0.4));
            }
        });

        return CastResult.success();
    }
}
