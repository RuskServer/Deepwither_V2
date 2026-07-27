package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class ColossusSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;
    private final VirtualHealthManager healthManager;

    @Inject
    public ColossusSkill(DamagePipelineManager damagePipelineManager, VirtualHealthManager healthManager) {
        this.damagePipelineManager = damagePipelineManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "colossus"; }

    @Override
    public String getDisplayName() { return "コロッサス"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "全身全霊の一撃を放ち、広範囲の敵を吹き飛ばす。",
                "周囲6mの敵に物理ダメージ(350%)と鈍足I(3秒)を与え、自身の最大HPを10%回復する。"
        );
    }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "heavy"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK, SkillTag.Role.CONTROL, SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST, SkillTag.Tactic.DISPLACE); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.CHANNELING, SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 50.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(25); }

    @Override
    public Duration getCastTime(SkillContext context) { return Duration.ofMillis(600); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var loc = player.getLocation();
        var world = loc.getWorld();

        // 予兆：地面に範囲リング
        TrailCircleHelper.spawnCircle(loc, 6.0, Color.fromRGB(180, 60, 30), 14, 36);
        TrailCircleHelper.spawnCircle(loc, 5.0, Color.fromRGB(200, 80, 40), 12, 30, new Vector(0, 1, 0), 30);
        world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);

        // 衝撃
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 1, 0), 3, 1.0, 0.5, 1.0, 0);

        // 広がる衝撃波リング
        TrailCircleHelper.spawnCircle(loc, 2.0, Color.fromRGB(200, 100, 50), 15, 20);
        TrailCircleHelper.spawnCircle(loc, 4.0, Color.fromRGB(180, 80, 40), 12, 28, new Vector(0, 1, 0), 45);
        TrailCircleHelper.spawnCircle(loc, 6.0, Color.fromRGB(160, 60, 30), 10, 36);

        world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 1, 0), 60, 6.0, 0.5, 6.0, 0.4, Material.STONE.createBlockData());
        world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.5, 0), 30, 5.0, 1.0, 5.0, 0.3, Material.DIRT.createBlockData());
        world.spawnParticle(Particle.CRIT, loc.clone().add(0, 0.5, 0), 40, 5.0, 0.5, 5.0, 0.2);
        world.spawnParticle(Particle.GLOW, loc.clone().add(0, 0.5, 0), 30, 4.0, 0.5, 4.0, 0.05);

        world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.4f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);

        player.getNearbyEntities(6.0, 6.0, 6.0).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                damagePipelineManager.processScaledDamage(player, living, DamageType.PHYSICAL, 3.5, getTags(), getId(), 500L);
                living.setVelocity(living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.0).setY(0.6));
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true));
            }
        });

        healthManager.heal(player, healthManager.getMaxHealth(player) * 0.1);

        return CastResult.success();
    }
}
