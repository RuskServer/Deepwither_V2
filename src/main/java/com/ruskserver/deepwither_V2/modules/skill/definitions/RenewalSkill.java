package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

@Component
public class RenewalSkill implements Skill {

    private final VirtualHealthManager healthManager;

    private final Map<UUID, BukkitRunnable> activeHoTs = new HashMap<>();

    @Inject
    public RenewalSkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "renewal"; }

    @Override
    public String getDisplayName() { return "再生"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "聖なる加護を味方に纏わせ、持続的に傷を塞ぐ。",
                "対象1体に6秒間、2秒ごとに最大HPの10%を回復する継続回復を付与する。"
        );
    }

    @Override
    public Material getIcon() { return Material.HONEY_BOTTLE; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "heal", "support", "hot"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 25.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(12); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();

        var ray = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                30, 1.0,
                e -> e instanceof LivingEntity && !e.equals(caster)
        );

        LivingEntity hitTarget = ray != null ? (LivingEntity) ray.getHitEntity() : null;
        final LivingEntity target = hitTarget != null ? hitTarget : caster;

        UUID targetId = target.getUniqueId();
        BukkitRunnable existing = activeHoTs.remove(targetId);
        if (existing != null) {
            existing.cancel();
        }

        var loc = target.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.3, 0.5, 0.3, 0.03);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.5f);

        BukkitRunnable task = new BukkitRunnable() {
            int tickCount = 0;
            final int TOTAL_TICKS = 80;
            final int INTERVAL = 40;

            @Override
            public void run() {
                if (tickCount >= TOTAL_TICKS || target.isDead() || !target.isValid()) {
                    cancel();
                    activeHoTs.remove(targetId, this);
                    return;
                }

                if (tickCount % INTERVAL == 0) {
                    double maxHp = healthManager.getMaxHealth(target);
                    healthManager.heal(target, maxHp * 0.1);
                    var healLoc = target.getLocation().add(0, 1, 0);
                    healLoc.getWorld().spawnParticle(Particle.HEART, healLoc, 4, 0.3, 0.3, 0.3, 0);
                    healLoc.getWorld().playSound(healLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.8f);
                }

                tickCount++;
            }
        };

        activeHoTs.put(targetId, task);
        task.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 1L);

        return CastResult.success();
    }
}
