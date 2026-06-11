package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
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

    @Override
    public String getId() { return "shield_wall"; }

    @Override
    public String getDisplayName() { return "シールドウォール"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "防御姿勢を取り、盾の力で自身を守る。",
                "自身に耐性II(6秒)を付与する。"
        );
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
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.DEFENSE, SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.LONG_CD); }

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

        return CastResult.success();
    }
}
