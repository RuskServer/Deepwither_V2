package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Color;
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
                "前方へ複数の雷撃を放ち、視線方向の敵を打ち抜く。",
                "最大7m先の敵に魔法ダメージ(100%)を与える。"
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
        Location eyeLoc = context.getEyeLocation();
        Vector direction = context.getDirection();

        for (int i = 0; i < 5; i++) {
            Vector spread = new Vector(
                    (Math.random() - 0.5) * 0.6,
                    (Math.random() - 0.5) * 0.3,
                    (Math.random() - 0.5) * 0.6
            );
            Vector boltDir = direction.clone().add(spread).normalize();
            double range = 3.0 + Math.random() * 4.0;
            Location boltLoc = eyeLoc.clone().add(boltDir.multiply(range));

            boltLoc.getWorld().spawnParticle(Particle.FLASH, boltLoc, 1, 0, 0, 0, 0, Color.WHITE);
            boltLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, boltLoc, 15, 0.3, 0.3, 0.3, 0.05);
        }

        eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);

        double range = 7.0;
        double angleCos = Math.cos(Math.toRadians(50));

        eyeLoc.getWorld().getNearbyEntities(eyeLoc, range, range, range).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                Vector toTarget = living.getLocation().toVector().subtract(eyeLoc.toVector());
                if (toTarget.length() <= range) {
                    double dot = toTarget.normalize().dot(direction);
                    if (dot >= angleCos) {
                        damagePipelineManager.processScaledDamage(context.getCaster(), living, DamageType.MAGIC, 1.0, getTags());
                        living.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, living.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        });

        return CastResult.success();
    }
}
