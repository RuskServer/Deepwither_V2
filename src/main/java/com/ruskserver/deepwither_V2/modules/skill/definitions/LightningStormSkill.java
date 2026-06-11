package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class LightningStormSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public LightningStormSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "lightning_storm"; }

    @Override
    public String getDisplayName() { return "ライトニングストーム"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "自身を中心に無数の雷撃を全方位に降り注がせる。",
                "周囲7mの敵に魔法ダメージ(100%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.FIREWORK_ROCKET; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "lightning", "area"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 35.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(6); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var center = player.getLocation();

        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);

        new BukkitRunnable() {
            int wave = 0;

            @Override
            public void run() {
                if (wave >= 3 || !player.isOnline()) {
                    cancel();
                    return;
                }

                int bolts = 6 + wave * 2;
                for (int i = 0; i < bolts; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = 1.5 + Math.random() * 5.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location boltLoc = center.clone().add(x, 0.5 + Math.random() * 2.0, z);

                    boltLoc.getWorld().spawnParticle(Particle.FLASH, boltLoc, 1, 0, 0, 0, 0);
                    boltLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, boltLoc, 12 + wave * 3, 0.3, 0.3, 0.3, 0.05);
                }

                center.getWorld().getNearbyEntities(center, 7.0, 5.0, 7.0).forEach(entity -> {
                    if (entity instanceof LivingEntity living && !entity.equals(player)) {
                        if (living.getLocation().distance(center) <= 7.0) {
                            living.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, living.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                });

                wave++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 6L);

        center.getWorld().getNearbyEntities(center, 7.0, 5.0, 7.0).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                if (living.getLocation().distance(center) <= 7.0) {
                    damagePipelineManager.processScaledDamage(player, living, DamageType.MAGIC, 1.0, getTags());
                }
            }
        });

        return CastResult.success();
    }
}
