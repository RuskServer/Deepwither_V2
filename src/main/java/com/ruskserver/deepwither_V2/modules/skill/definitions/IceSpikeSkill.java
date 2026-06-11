package com.ruskserver.deepwither_V2.modules.skill.definitions;

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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class IceSpikeSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public IceSpikeSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "ice_spike"; }

    @Override
    public String getDisplayName() { return "アイススパイク"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方の地面を走るように無数の氷の棘を噴出させる。",
                "視線方向へ最大10mまで氷柱が連続して噴出し、各地点周囲2.5mの敵に魔法ダメージ(180%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.PACKED_ICE; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "ice", "area"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 45.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(8); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var origin = player.getEyeLocation();
        var dir = context.getDirection();

        dir.setY(0).normalize();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);

        for (double d = 1.0; d <= 10.0; d += 1.5) {
            var spikeLoc = origin.clone().add(dir.clone().multiply(d));
            spikeLoc.setY(spikeLoc.getY() - 0.5);

            for (int h = 0; h < 2; h++) {
                double height = h * 0.8;
                spikeLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, spikeLoc.clone().add(0, height, 0), 12, 0.4, 0.3, 0.4, 0.03);
                spikeLoc.getWorld().spawnParticle(Particle.POOF, spikeLoc.clone().add(0, height, 0), 3, 0.2, 0.1, 0.2, 0.0);
            }

            spikeLoc.getWorld().playSound(spikeLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.4f + (float) d * 0.05f);

            for (Entity entity : spikeLoc.getWorld().getNearbyEntities(spikeLoc, 2.5, 3.0, 2.5)) {
                if (entity instanceof LivingEntity living && !entity.equals(player)) {
                    if (living.getNoDamageTicks() <= 10) {
                        damagePipelineManager.processScaledDamage(player, living, DamageType.MAGIC, 1.8, getTags());
                        living.setNoDamageTicks(10);
                    }
                }
            }
        }

        return CastResult.success();
    }
}
