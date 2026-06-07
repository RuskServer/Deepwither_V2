package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class FortressSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;
    private final VirtualHealthManager healthManager;

    @Inject
    public FortressSkill(DamagePipelineManager damagePipelineManager, VirtualHealthManager healthManager) {
        this.damagePipelineManager = damagePipelineManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "fortress"; }

    @Override
    public String getDisplayName() { return "フォートレス"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "大地を踏み鳴らし、周囲の敵を自身へ引き寄せる。",
                "周囲7mの敵に魔法ダメージ(150%)を与え、自身に耐性III(3秒)と最大HP20%回復を付与する。"
        );
    }

    @Override
    public Material getIcon() { return Material.ANVIL; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "defense"); }

    @Override
    public double getManaCost(SkillContext context) { return 50.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(30); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var loc = player.getLocation().add(0, 1, 0);

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 40, 2.0, 0.5, 2.0, 0.2);
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.6f);

        player.getNearbyEntities(7.0, 7.0, 7.0).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                damagePipelineManager.processScaledDamage(player, living, DamageType.MAGIC, 1.5, getTags());
                living.setVelocity(player.getLocation().toVector().subtract(living.getLocation().toVector()).normalize().multiply(0.8).setY(0.3));
            }
        });

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2, false, true));
        healthManager.heal(player, healthManager.getMaxHealth(player) * 0.2);

        return CastResult.success();
    }
}
