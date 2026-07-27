package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import org.bukkit.Color;
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
public class IceShardSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public IceShardSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "ice_shard"; }

    @Override
    public String getDisplayName() { return "アイスシャード"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方に3つの氷の破片を高速で放ち、命中時に衝撃波を発生させる。",
                "各破片が周囲1.5mに魔法ダメージ(50%)を与え、3つすべて命中で150%になる。"
        );
    }

    @Override
    public Material getIcon() { return Material.ICE; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.PROJECTILE; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "ice", "projectile"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL, SkillTag.Scaling.CDR_HEAVY); }

    @Override
    public double getManaCost(SkillContext context) { return 20.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(2); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var eyeLoc = context.getEyeLocation();
        var baseDir = context.getDirection().clone();
        double damagePerShard = 0.5;

        // 視点中央から3方向にわずかに拡散
        double[] yawOffsets = {-3.0, 0.0, 3.0};

        for (double yawOffset : yawOffsets) {
            var shardDir = rotateYaw(baseDir, yawOffset);

            SkillProjectile projectile = new SkillProjectile(
                    player,
                    eyeLoc.clone().add(shardDir.clone().multiply(0.6)),
                    shardDir,
                    1.8,
                    0.6,
                    50
            ) {
                @Override
                protected void onTick() {
                    var loc = getCurrentLocation();
                    var world = loc.getWorld();
                    world.spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.1, 0.1, 0.1, 0.02);
                    world.spawnParticle(Particle.POOF, loc, 1, 0.05, 0.05, 0.05, 0.01);

                    TrailCircleHelper.spawnCircle(loc, 0.2, Color.fromRGB(200, 230, 255), 4, 4, shardDir, getTicksLived() * 45);
                }

                @Override
                protected void onHitEntity(LivingEntity target) {
                    impact(getCurrentLocation());
                    remove();
                }

                @Override
                protected void onHitBlock(Block block) {
                    impact(getCurrentLocation());
                    remove();
                }

                private void impact(Location loc) {
                    var world = loc.getWorld();
                    world.spawnParticle(Particle.POOF, loc, 8, 0.3, 0.3, 0.3, 0.08);
                    world.spawnParticle(Particle.SNOWFLAKE, loc, 12, 0.4, 0.4, 0.4, 0.08);
                    world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.5f);

                    TrailCircleHelper.spawnCircle(loc, 0.8, Color.fromRGB(180, 220, 255), 6, 10);
                    TrailCircleHelper.spawnCircle(loc, 0.5, Color.fromRGB(220, 240, 255), 5, 8, new Vector(0, 1, 0), 45);

                    world.getNearbyEntities(loc, 1.5, 1.5, 1.5).forEach(entity -> {
                        if (entity instanceof LivingEntity living && !entity.equals(player)) {
                            damagePipelineManager.processScaledDamage(player, living, DamageType.MAGIC, damagePerShard, getTags(), getId(), 0L);
                        }
                    });
                }
            };

            if (!projectileService.launch(projectile)) {
                return CastResult.fail();
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.8f, 1.2f);
        return CastResult.success();
    }

    private static Vector rotateYaw(Vector v, double degrees) {
        return v.clone().rotateAroundY(Math.toRadians(degrees));
    }
}
