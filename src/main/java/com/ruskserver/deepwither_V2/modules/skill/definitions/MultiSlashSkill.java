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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class MultiSlashSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public MultiSlashSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "multi_slash"; }

    @Override
    public String getDisplayName() { return "マルチスラッシュ"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方に鋭い斬撃の嵐を放ち、敵を切り刻む。",
                "前方の敵に0.15秒ごとに物理ダメージ(35%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.IRON_SWORD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("physical", "melee", "multi_hit"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 25.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(8); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();

        new BukkitRunnable() {
            int count = 0;
            final int MAX_HITS = 5;

            @Override
            public void run() {
                if (count >= MAX_HITS || !player.isValid() || player.isDead()) {
                    cancel();
                    return;
                }

                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                Location center = start.clone().add(dir.clone().multiply(2.5));

                center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.2f + count * 0.1f);

                for (int i = 0; i < 5; i++) {
                    double offsetX = (Math.random() - 0.5) * 3.0;
                    double offsetY = (Math.random() - 0.5) * 2.0;
                    double offsetZ = (Math.random() - 0.5) * 3.0;
                    Location slashLoc = center.clone().add(offsetX, offsetY, offsetZ);
                    center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, slashLoc, 1);
                    center.getWorld().spawnParticle(Particle.CRIT, slashLoc, 5, 0.2, 0.2, 0.2, 0.1);
                }

                center.getWorld().getNearbyEntities(center, 3.5, 2.5, 3.5).forEach(entity -> {
                    if (entity instanceof LivingEntity living && !entity.equals(player)) {
                        damagePipelineManager.processScaledDamage(player, living, DamageType.PHYSICAL, 0.35, getTags(), getId(), 0L);
                    }
                });

                count++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 3L);

        return CastResult.success();
    }
}
