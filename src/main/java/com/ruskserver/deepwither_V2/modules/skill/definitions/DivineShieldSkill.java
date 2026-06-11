package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import com.ruskserver.deepwither_V2.Deepwither_V2;

import java.time.Duration;
import java.util.*;

@Component
public class DivineShieldSkill implements Skill {

    private final VirtualHealthManager healthManager;
    private final Map<UUID, BukkitRunnable> activeShields = new HashMap<>();

    @Inject
    public DivineShieldSkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "divine_shield"; }

    @Override
    public String getDisplayName() { return "聖盾"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "聖なる力を壁状に具現化し、身を守る障壁とする。",
                "自身に最大HPの30%のダメージ吸収バリア(8秒)を付与する。"
        );
    }

    @Override
    public Material getIcon() { return Material.SHIELD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "defense", "barrier", "support"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.DEFENSE, SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 25.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(16); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();

        UUID id = caster.getUniqueId();
        BukkitRunnable existing = activeShields.remove(id);
        if (existing != null) {
            existing.cancel();
        }
        healthManager.removeBarrier(caster);

        double barrierAmount = healthManager.getMaxHealth(caster) * 0.3;
        healthManager.setBarrier(caster, barrierAmount);

        var loc = caster.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 25, 0.5, 0.6, 0.5, 0.08);
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 20, 0.5, 0.5, 0.5, 0);
        loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.3f);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            final int MAX_TICKS = 160;

            @Override
            public void run() {
                if (ticks >= MAX_TICKS) {
                    healthManager.removeBarrier(caster);
                    activeShields.remove(id);
                    cancel();
                    return;
                }

                if (healthManager.getBarrier(caster) <= 0) {
                    activeShields.remove(id);
                    cancel();
                    return;
                }

                if (ticks % 4 == 0) {
                    caster.getWorld().spawnParticle(Particle.END_ROD, caster.getLocation().add(0, 0.5, 0), 1, 0.4, 0.5, 0.4, 0.02);
                }

                ticks++;
            }
        };

        activeShields.put(id, task);
        task.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 1L);

        return CastResult.success();
    }
}
