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
public class BarrageSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public BarrageSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "barrage"; }

    @Override
    public String getDisplayName() { return "乱れ撃ち"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方に向かって無数の矢をばらまき、広範囲を制圧する。",
                "扇状に5本の矢を放ち、命中した敵1体につき物理ダメージ(60%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.FIREWORK_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.PROJECTILE; }

    @Override
    public Set<String> getTags() { return Set.of("ranged", "archer", "aoe", "rapid"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL, SkillTag.Scaling.CDR_HEAVY); }

    @Override
    public double getManaCost(SkillContext context) { return 20.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(6); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var spawnLoc = context.getEyeLocation().add(context.getDirection().multiply(0.8));
        var baseDir = context.getDirection();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.2f, 0.9f);

        double[] angles = { -0.25, -0.12, 0.0, 0.12, 0.25 };
        for (double angle : angles) {
            var dir = rotateYaw(baseDir, angle);
            launchArrow(player, spawnLoc, dir);
        }

        spawnLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, spawnLoc, 15, 0.4, 0.1, 0.4, 0);
        spawnLoc.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 30, 0.5, 0.2, 0.5, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.8f, 1.3f);

        return CastResult.success();
    }

    private void launchArrow(LivingEntity player, Location spawnLoc, Vector direction) {
        var projectile = new SkillProjectile(player, spawnLoc, direction, 1.6, 0.4, 25) {
            @Override
            protected void onTick() {
                var loc = getCurrentLocation();
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.03, 0.03, 0.03, 0.02);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                damagePipelineManager.processScaledDamage(getCaster(), target, DamageType.PHYSICAL, 0.6, getTags());
                getCurrentLocation().getWorld().spawnParticle(Particle.CRIT, getCurrentLocation(), 8, 0.15, 0.15, 0.15, 0.1);
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                getCurrentLocation().getWorld().spawnParticle(Particle.CLOUD, getCurrentLocation(), 4, 0.08, 0.08, 0.08, 0.02);
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
