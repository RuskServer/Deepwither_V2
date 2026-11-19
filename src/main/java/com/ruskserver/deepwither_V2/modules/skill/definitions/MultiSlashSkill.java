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
import com.ruskserver.deepwither_V2.modules.skill.util.AnimeVfxHelper;
import org.bukkit.Color;
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

                Location origin = player.getLocation().add(0, 1.15, 0);
                Vector rawDir = player.getEyeLocation().getDirection().setY(0);
                final Vector dir = rawDir.lengthSquared() < 1e-6
                        ? new Vector(0, 0, 1)
                        : rawDir.normalize();

                boolean isFinisher = (count == MAX_HITS - 1);
                double slashRadius = isFinisher ? 4.2 : 3.6;

                var plugin = JavaPlugin.getPlugin(Deepwither_V2.class);
                double[] rollAngles = {-32.0, 32.0, -18.0, 42.0, 0.0};
                Color slashColor = isFinisher
                        ? Color.fromRGB(255, 170, 70)
                        : Color.fromRGB(255, 232, 145);

                // プレイヤー中心から前方へ薙ぎ払う三日月斬撃（判定半径と完全一致）
                AnimeVfxHelper.spawnCrescentSlash(plugin, origin, dir, slashRadius,
                        slashColor, rollAngles[count % rollAngles.length], 5);

                Location frontCenter = origin.clone().add(dir.clone().multiply(slashRadius * 0.55));
                origin.getWorld().playSound(frontCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.85f, 1.15f + count * 0.1f);

                double radiusSq = slashRadius * slashRadius;
                origin.getWorld().getNearbyEntities(origin, slashRadius, 2.5, slashRadius).forEach(entity -> {
                    if (entity instanceof LivingEntity living && !entity.equals(player)) {
                        Vector toTarget = living.getLocation().toVector().subtract(origin.toVector()).setY(0);
                        double distSq = toTarget.lengthSquared();
                        if (distSq > radiusSq) return;
                        // 前方約 200° の弧内のみヒット（背後の敵には当たらない）
                        if (distSq > 0.25 && toTarget.normalize().dot(dir) < -0.17) return;

                        damagePipelineManager.processScaledDamage(player, living, DamageType.PHYSICAL, 0.35,
                                getTags(), getId(), 0L, null, 0.3);
                        Location hitCenter = living.getLocation().add(0, living.getHeight() * 0.55, 0);
                        AnimeVfxHelper.spawnHitSlashCross(plugin, hitCenter, slashColor, isFinisher ? 2.6 : 1.9, 5);
                        if (isFinisher) {
                            AnimeVfxHelper.spawnImpactBurst(plugin, hitCenter, 1.4, Color.fromRGB(255, 200, 95), 5);
                        }
                    }
                });

                count++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 3L);

        return CastResult.success();
    }
}
