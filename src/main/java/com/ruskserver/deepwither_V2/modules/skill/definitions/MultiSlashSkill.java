package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Bukkit;
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
public class MultiSlashSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;
    private final Deepwither_V2 plugin;

    @Inject
    public MultiSlashSkill(DamagePipelineManager damagePipelineManager, Deepwither_V2 plugin) {
        this.damagePipelineManager = damagePipelineManager;
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "multi_slash"; }

    @Override
    public String getDisplayName() { return "連撃"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方に3連続の斬撃を繰り出し、扇状の敵を切り刻む。",
                "各斬撃は最大4m先の敵に物理ダメージ(75%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.DIAMOND_SWORD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "technique"); }

    @Override
    public double getManaCost(SkillContext context) { return 25.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(6); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var eyeLoc = player.getEyeLocation();
        var dir = context.getDirection();

        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int hits = 0;

            @Override
            public void run() {
                if (hits >= 3) { return; }
                hits++;

                Location origin = eyeLoc.clone().add(dir.clone().multiply(1.0 + hits));
                origin.getWorld().spawnParticle(Particle.SWEEP_ATTACK, origin, 1, 0.3, 0.3, 0.3, 0);
                origin.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.9f + hits * 0.1f);

                double range = 4.0;
                double angleCos = Math.cos(Math.toRadians(60));

                origin.getWorld().getNearbyEntities(origin, range, range, range).forEach(entity -> {
                    if (entity instanceof LivingEntity living && !entity.equals(player)) {
                        Vector toTarget = living.getLocation().toVector().subtract(eyeLoc.toVector());
                        if (toTarget.length() <= range) {
                            double dot = toTarget.normalize().dot(dir);
                            if (dot >= angleCos) {
                                damagePipelineManager.processScaledDamage(player, living, DamageType.PHYSICAL, 0.75, getTags());
                            }
                        }
                    }
                });
            }
        }, 0L, 3L);

        return CastResult.success();
    }
}
