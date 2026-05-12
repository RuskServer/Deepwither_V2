package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class ShieldWallSkill implements Skill {

    private final VirtualHealthManager healthManager;

    @Inject
    public ShieldWallSkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "shield_wall"; }

    @Override
    public String getDisplayName() { return "シールドウォール"; }

    @Override
    public List<String> getDescription() {
        return List.of("防御姿勢をとり、受けるダメージを6秒間減少させる。");
    }

    @Override
    public Material getIcon() { return Material.SHIELD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "defense"); }

    @Override
    public double getManaCost(SkillContext context) { return 25.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(12); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var loc = player.getLocation().add(0, 1, 0);

        loc.getWorld().spawnParticle(Particle.CRIT, loc, 20, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1, false, true));
        healthManager.heal(player, healthManager.getMaxHealth(player) * 0.1);

        return CastResult.success();
    }
}
