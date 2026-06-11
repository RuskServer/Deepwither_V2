package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class SanctuarySkill implements Skill {

    private final VirtualHealthManager healthManager;

    @Inject
    public SanctuarySkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "sanctuary"; }

    @Override
    public String getDisplayName() { return "聖域"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "聖なる光で地面に円を描き、その範囲に立つ者を癒す結界を張る。",
                "6秒間、半径5mの範囲にいる味方に毎秒最大HPの8%を回復する。"
        );
    }

    @Override
    public Material getIcon() { return Material.LIGHT; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.LOCATION; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "heal", "support", "area"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 40.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(18); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();

        Location center = context.getTargetLocation();
        if (center == null) {
            var start = caster.getEyeLocation();
            var dir = start.getDirection();
            var result = caster.getWorld().rayTraceBlocks(start, dir, 10.0, FluidCollisionMode.NEVER, false);
            if (result != null && result.getHitBlock() != null) {
                center = result.getHitPosition().toLocation(caster.getWorld());
            } else {
                center = start.clone().add(dir.multiply(10.0));
            }
        }

        final Location targetCenter = center.add(0, 0.5, 0);

        targetCenter.getWorld().spawnParticle(Particle.END_ROD, targetCenter, 30, 2.5, 0.3, 2.5, 0.05);
        targetCenter.getWorld().playSound(targetCenter, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.9f);

        new BukkitRunnable() {
            int ticks = 0;
            final int MAX_TICKS = 120;

            @Override
            public void run() {
                if (ticks >= MAX_TICKS) {
                    cancel();
                    return;
                }

                if (ticks % 5 == 0) {
                    targetCenter.getWorld().spawnParticle(Particle.END_ROD, targetCenter, 5, 2.5, 0.2, 2.5, 0.02);
                    targetCenter.getWorld().spawnParticle(Particle.ENCHANT, targetCenter, 8, 2.5, 0.5, 2.5, 0);
                }

                if (ticks % 20 == 0) {
                    targetCenter.getWorld().getNearbyEntities(targetCenter, 5.0, 5.0, 5.0).forEach(entity -> {
                        if (entity instanceof Player ally) {
                            double maxHp = healthManager.getMaxHealth(ally);
                            healthManager.heal(ally, maxHp * 0.08);
                            var allyLoc = ally.getLocation().add(0, 1, 0);
                            allyLoc.getWorld().spawnParticle(Particle.HEART, allyLoc, 3, 0.3, 0.3, 0.3, 0);
                        }
                    });
                    targetCenter.getWorld().playSound(targetCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.6f);
                }

                if (ticks % 2 == 0) {
                    targetCenter.getWorld().spawnParticle(Particle.END_ROD, targetCenter, 1, 2.5, 0.1, 2.5, 0.01);
                }

                ticks++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 1L);

        return CastResult.success();
    }
}
