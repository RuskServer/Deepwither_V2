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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class IceSpikeSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public IceSpikeSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "ice_spike"; }

    @Override
    public String getDisplayName() { return "アイススパイク"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "氷の棘を放ち、命中した地点で氷柱を噴出させる。",
                "直進する棘が命中した地点を中心に周囲2.5mの敵に魔法ダメージ(300%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.PACKED_ICE; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.PROJECTILE; }

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
        var spawnLoc = context.getEyeLocation().add(context.getDirection().multiply(0.8));
        var direction = context.getDirection();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.4f);

        var projectile = new SkillProjectile(player, spawnLoc, direction, 1.8, 0.8, 25) {
            @Override
            protected void onTick() {
                var loc = getCurrentLocation();
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 6, 0.08, 0.08, 0.08, 0.02);
                loc.getWorld().spawnParticle(Particle.POOF, loc, 2, 0.05, 0.05, 0.05, 0.0);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                explode(getCurrentLocation());
            }

            @Override
            protected void onHitBlock(Block block) {
                explode(getCurrentLocation());
            }

            private void explode(Location loc) {
                var world = loc.getWorld();
                if (world == null) return;

                for (int i = 0; i < 5; i++) {
                    double height = i * 1.0;
                    world.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, height, 0), 20, 0.3, 0.5, 0.3, 0.05);
                    world.spawnParticle(Particle.POOF, loc.clone().add(0, height, 0), 5, 0.2, 0.2, 0.2, 0.0);
                }
                world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);

                for (Entity entity : world.getNearbyEntities(loc, 2.5, 5.0, 2.5)) {
                    if (entity instanceof LivingEntity living && !entity.equals(getCaster())) {
                        damagePipelineManager.processScaledDamage(getCaster(), living, DamageType.MAGIC, 3.0, getTags());
                    }
                }

                remove();
            }
        };

        boolean launched = projectileService.launch(projectile);
        return launched ? CastResult.success() : CastResult.fail();
    }
}
