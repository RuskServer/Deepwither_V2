package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class BattleCrySkill implements Skill {

    private final VirtualHealthManager healthManager;

    @Inject
    public BattleCrySkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "battle_cry"; }

    @Override
    public String getDisplayName() { return "バトルクライ"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "雄叫びを上げ、自身と周囲の味方を鼓舞する。",
                "自身に攻撃力上昇I(6秒)、周囲8mの味方に最大HP15%回復と攻撃力上昇I(6秒)を付与する。"
        );
    }

    @Override
    public Material getIcon() { return Material.GOAT_HORN; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "defense"); }

    @Override
    public double getManaCost(SkillContext context) { return 30.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(18); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var loc = player.getLocation().add(0, 1, 0);

        loc.getWorld().spawnParticle(Particle.NOTE, loc, 20, 1.5, 0.5, 1.5, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        player.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 120, 0, false, true));

        player.getNearbyEntities(8.0, 8.0, 8.0).forEach(entity -> {
            if (entity instanceof Player ally && !ally.equals(player)) {
                healthManager.heal(ally, healthManager.getMaxHealth(ally) * 0.15);
                ally.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 120, 0, false, true));
                ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
            }
        });

        return CastResult.success();
    }
}
