package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class ThunderBlastSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;
    private final Deepwither_V2 plugin;

    @Inject
    public ThunderBlastSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager, Deepwither_V2 plugin) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "thunder_blast"; }

    @Override
    public String getDisplayName() { return "サンダーブラスト"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "0.5秒の詠唱後に雷球を放ち、着弾後わずかに遅れて爆発する。",
                "周囲5mの敵に魔法ダメージ(450%)と鈍足II(3秒)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.PROJECTILE; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "lightning", "area"); }

    @Override
    public double getManaCost(SkillContext context) { return 70.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(14); }

    @Override
    public Duration getCastTime(SkillContext context) { return Duration.ofMillis(500); }

    @Override
    public CastResult cast(SkillContext context) {
        SkillProjectile projectile = new SkillProjectile(
                context.getCaster(),
                context.getEyeLocation().add(context.getDirection().multiply(0.6)),
                context.getDirection(),
                0.8,
                0.8,
                100
        ) {
            @Override
            protected void onTick() {
                getCurrentLocation().getWorld().spawnParticle(Particle.ELECTRIC_SPARK, getCurrentLocation(), 4, 0.15, 0.15, 0.15, 0.03);
                getCurrentLocation().getWorld().spawnParticle(Particle.CRIT, getCurrentLocation(), 1, 0.05, 0.05, 0.05, 0.01);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                detonate(target.getLocation());
                remove();
            }

            @Override
            protected void onHitBlock(Block block) {
                detonate(getCurrentLocation());
                remove();
            }

            private void detonate(Location location) {
                location.getWorld().spawnParticle(Particle.FLASH, location, 1, 0, 0, 0, 0, Color.WHITE);
                location.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    location.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, location, 1, 0, 0, 0, 0);
                    location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 40, 1.0, 1.0, 1.0, 0.15);
                    location.getWorld().spawnParticle(Particle.FLASH, location, 3, 0.5, 0.5, 0.5, 0, Color.WHITE);
                    location.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);

                    location.getWorld().getNearbyEntities(location, 5.0, 5.0, 5.0).forEach(entity -> {
                        if (entity instanceof LivingEntity living && !entity.equals(context.getCaster())) {
                            damagePipelineManager.processScaledDamage(context.getCaster(), living, DamageType.MAGIC, 4.5, getTags());
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
                        }
                    });
                }, 6L);
            }
        };

        if (projectileService.launch(projectile)) {
            context.getCaster().playSound(context.getCaster().getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
            return CastResult.success();
        }
        return CastResult.fail();
    }
}
