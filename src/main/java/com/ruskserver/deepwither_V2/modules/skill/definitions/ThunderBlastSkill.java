package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
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
import org.bukkit.util.Vector;

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
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK, SkillTag.Role.CONTROL); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.CHANNELING, SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 70.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(14); }

    @Override
    public Duration getCastTime(SkillContext context) { return Duration.ofMillis(500); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var eyeLoc = context.getEyeLocation();
        var dir = context.getDirection().clone();

        SkillProjectile projectile = new SkillProjectile(
                player,
                eyeLoc.add(dir.clone().multiply(0.6)),
                dir,
                0.8,
                0.8,
                100
        ) {
            @Override
            protected void onTick() {
                var loc = getCurrentLocation();
                var world = loc.getWorld();

                // 雷球本体
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 4, 0.15, 0.15, 0.15, 0.03);
                world.spawnParticle(Particle.CRIT, loc, 1, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.GLOW, loc, 2, 0.1, 0.1, 0.1, 0);

                // 軌道に沿った電撃リング
                TrailCircleHelper.spawnCircle(loc, 0.3, Color.fromRGB(100, 149, 237), 4, 6, dir, getTicksLived() * 30);
                TrailCircleHelper.spawnCircle(loc, 0.5, Color.fromRGB(70, 130, 255), 4, 8, dir, getTicksLived() * 30 + 45);
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
                var world = location.getWorld();

                // 着弾直撃：衝撃波リング + 閃光
                world.spawnParticle(Particle.FLASH, location, 1, 0, 0, 0, 0, Color.WHITE);
                world.spawnParticle(Particle.SONIC_BOOM, location, 5, 0.5, 0.5, 0.5, 0);
                world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);

                TrailCircleHelper.spawnCircle(location, 1.0, Color.fromRGB(100, 149, 237), 10, 16);
                TrailCircleHelper.spawnCircle(location, 1.5, Color.fromRGB(70, 130, 255), 8, 20, new Vector(0, 1, 0), 45);

                // 蓄電 → 爆発（6tick後）
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // メイン爆発
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, location, 60, 2.0, 2.0, 2.0, 0.2);
                    world.spawnParticle(Particle.FLASH, location, 5, 1.0, 1.0, 1.0, 0, Color.WHITE);
                    world.spawnParticle(Particle.GLOW, location, 80, 3.0, 3.0, 3.0, 0.05);
                    world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);

                    // 拡大する複数の衝撃波リング
                    for (int i = 0; i < 4; i++) {
                        double radius = 1.5 + i * 1.0;
                        TrailCircleHelper.spawnCircle(location, radius, Color.fromRGB(100, 149, 237), 12 - i, 20 + i * 4);
                        TrailCircleHelper.spawnCircle(location, radius - 0.3, Color.fromRGB(70, 130, 255), 10 - i, 16 + i * 4, new Vector(0, 1, 0), 30);
                    }

                    // ダメージ・鈍足
                    world.getNearbyEntities(location, 5.0, 5.0, 5.0).forEach(entity -> {
                        if (entity instanceof LivingEntity living && !entity.equals(player)) {
                            damagePipelineManager.processScaledDamage(player, living, DamageType.MAGIC, 4.5,
                                    getTags(), getId(), 500L, location);
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
                        }
                    });
                }, 6L);
            }
        };

        if (projectileService.launch(projectile)) {
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
            return CastResult.success();
        }
        return CastResult.fail();
    }
}
