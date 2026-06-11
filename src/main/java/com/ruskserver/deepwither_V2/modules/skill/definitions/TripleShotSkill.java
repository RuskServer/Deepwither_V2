package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillProjectile;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class TripleShotSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public TripleShotSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "triple_shot"; }

    @Override
    public String getDisplayName() { return "三連射"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "息を整え、三本の矢を素早く放つ。",
                "3本の矢が前方へ扇状に飛翔し、命中した敵1体につき物理ダメージ(70%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.BOW; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.PROJECTILE; }

    @Override
    public Set<String> getTags() { return Set.of("ranged", "archer", "rapid"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL, SkillTag.Scaling.CDR_HEAVY); }

    @Override
    public double getManaCost(SkillContext context) { return 14.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(5); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var spawnLoc = context.getEyeLocation().add(context.getDirection().multiply(0.8));
        var baseDir = context.getDirection();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

        double[] angles = { -0.12, 0.0, 0.12 };
        for (double angle : angles) {
            var dir = rotateYaw(baseDir, angle);
            launchArrow(player, spawnLoc, dir);
        }

        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, spawnLoc, 8, 0.3, 0.1, 0.3, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.2f);

        return CastResult.success();
    }

    private void launchArrow(LivingEntity player, Location spawnLoc, Vector direction) {
        var projectile = new SkillProjectile(player, spawnLoc, direction, 1.8, 0.5, 30) {
            @Override
            protected void onTick() {
                var loc = getCurrentLocation();
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 4, 0.04, 0.04, 0.04, 0.02);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                damagePipelineManager.processScaledDamage(getCaster(), target, DamageType.RANGED, 0.7, getTags());
                getCurrentLocation().getWorld().spawnParticle(Particle.CRIT, getCurrentLocation(), 10, 0.15, 0.15, 0.15, 0.1);
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                getCurrentLocation().getWorld().spawnParticle(Particle.CLOUD, getCurrentLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                remove();
            }
        };
        projectileService.launch(projectile);
    }

    private Vector rotateYaw(Vector dir, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = dir.getX() * cos - dir.getZ() * sin;
        double z = dir.getX() * sin + dir.getZ() * cos;
        return new Vector(x, dir.getY(), z).normalize();
    }
}
