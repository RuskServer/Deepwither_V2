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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class EndlessVolleySkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public EndlessVolleySkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "endless_volley"; }

    @Override
    public String getDisplayName() { return "連射"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "矢をつがえながら次々と放ち、高速連射を叩き込む。",
                "0.6秒間で5本の矢を連続して放ち、命中した敵1体につき物理ダメージ(50%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.ARROW; }

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
    public double getManaCost(SkillContext context) { return 22.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(7); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);

        new BukkitRunnable() {
            int shot = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || shot >= 5) {
                    if (shot >= 5) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.5f);
                    }
                    cancel();
                    return;
                }

                var spawnLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.8));
                var dir = player.getEyeLocation().getDirection();
                launchArrow(player, spawnLoc, dir);

                spawnLoc.getWorld().spawnParticle(Particle.CRIT, spawnLoc, 6, 0.1, 0.1, 0.1, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.1f + shot * 0.1f);

                shot++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 3L);

        return CastResult.success();
    }

    private void launchArrow(LivingEntity player, Location spawnLoc, Vector direction) {
        var projectile = new SkillProjectile(player, spawnLoc, direction, 1.8, 0.4, 25) {
            @Override
            protected void onTick() {
                var loc = getCurrentLocation();
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.03, 0.03, 0.03, 0.01);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.02, 0.02, 0.02, 0.01);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                damagePipelineManager.processScaledDamage(getCaster(), target, DamageType.PHYSICAL, 0.5, getTags());
                getCurrentLocation().getWorld().spawnParticle(Particle.CRIT, getCurrentLocation(), 8, 0.12, 0.12, 0.12, 0.08);
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                getCurrentLocation().getWorld().spawnParticle(Particle.CLOUD, getCurrentLocation(), 3, 0.06, 0.06, 0.06, 0.02);
                remove();
            }
        };
        projectileService.launch(projectile);
    }
}
