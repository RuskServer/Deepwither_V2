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
public class FlamePillarSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public FlamePillarSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "flame_pillar"; }

    @Override
    public String getDisplayName() { return "フレイムピラー"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方の地面を焼き尽くすように無数の火柱を噴出させる。",
                "視線方向へ最大10mまで火柱が連続して噴出し、各地点周囲2.5mの敵に魔法ダメージ(180%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.BLAZE_ROD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "fire", "area"); }

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

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.6f, 1.5f);

        for (double d = 1.0; d <= 10.0; d += 1.5) {
            var pillarLoc = origin.clone().add(dir.clone().multiply(d));
            pillarLoc.setY(pillarLoc.getY() - 0.5);

            for (int h = 0; h < 4; h++) {
                double height = h * 0.8;
                pillarLoc.getWorld().spawnParticle(Particle.FLAME, pillarLoc.clone().add(0, height, 0), 20, 0.4, 0.3, 0.4, 0.03);
                pillarLoc.getWorld().spawnParticle(Particle.LAVA, pillarLoc.clone().add(0, height, 0), 3, 0.2, 0.1, 0.2, 0.0);
                pillarLoc.getWorld().spawnParticle(Particle.CLOUD, pillarLoc.clone().add(0, height, 0), 4, 0.2, 0.1, 0.2, 0.02);
            }

            pillarLoc.getWorld().playSound(pillarLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 0.9f + (float) d * 0.05f);

            for (Entity entity : pillarLoc.getWorld().getNearbyEntities(pillarLoc, 2.5, 3.0, 2.5)) {
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
