package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class ShockwaveSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;
    private final Deepwither_V2 plugin;

    @Inject
    public ShockwaveSkill(DamagePipelineManager damagePipelineManager, Deepwither_V2 plugin) {
        this.damagePipelineManager = damagePipelineManager;
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "shockwave"; }

    @Override
    public String getDisplayName() { return "衝撃波"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方へ衝撃波を放ち、進路上の敵を吹き飛ばす。",
                "最大8m先の敵に物理ダメージ(100%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.HEART_OF_THE_SEA; }

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
    public double getManaCost(SkillContext context) { return 30.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(8); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var startLoc = player.getEyeLocation();
        var dir = context.getDirection().normalize();

        context.getCaster().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);

        new BukkitRunnable() {
            double distance = 0;
            final double maxDistance = 8.0;
            final java.util.Set<LivingEntity> hit = new java.util.HashSet<>();

            @Override
            public void run() {
                if (distance >= maxDistance) { this.cancel(); return; }

                Location current = startLoc.clone().add(dir.clone().multiply(distance));
                current.getWorld().spawnParticle(Particle.CRIT, current, 5, 0.5, 0.3, 0.5, 0.05);

                current.getWorld().getNearbyEntities(current, 2.0, 2.0, 2.0).forEach(entity -> {
                    if (entity instanceof LivingEntity living && !entity.equals(player) && !hit.contains(living)) {
                        hit.add(living);
                        damagePipelineManager.processScaledDamage(player, living, DamageType.PHYSICAL, 1.0, getTags());
                        living.setVelocity(dir.clone().multiply(1.5).setY(0.3));
                    }
                });

                distance += 0.8;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return CastResult.success();
    }
}
