package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
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
public class SeismicStompSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public SeismicStompSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "seismic_stomp"; }

    @Override
    public String getDisplayName() { return "サイズミックストンプ"; }

    @Override
    public List<String> getDescription() {
        return List.of("地面を踏み鳴らし、周囲の敵にダメージと移動速度低下を与える。");
    }

    @Override
    public Material getIcon() { return Material.IRON_BOOTS; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "heavy"); }

    @Override
    public double getManaCost(SkillContext context) { return 30.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(10); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var loc = player.getLocation();

        loc.getWorld().spawnParticle(Particle.BLOCK, loc.add(0, 0.2, 0), 40, 2.0, 0.1, 2.0, 0.3, org.bukkit.Material.STONE.createBlockData());
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 20, 2.0, 0.1, 2.0, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);

        double damage = 35.0 + (context.getLevel() * 7.0);
        player.getNearbyEntities(4.5, 4.5, 4.5).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                damagePipelineManager.processDamage(player, living, DamageType.PHYSICAL, damage, getTags());
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, true));
            }
        });

        return CastResult.success();
    }
}
