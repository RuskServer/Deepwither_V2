package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import com.ruskserver.deepwither_V2.modules.skill.util.AnimeVfxHelper;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class WhirlwindSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public WhirlwindSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "whirlwind"; }

    @Override
    public String getDisplayName() { return "旋風斬り"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "その場で回転斬りを放ち、周囲の敵を斬り払う。",
                "周囲3.5mの敵に物理ダメージ(125%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.IRON_SWORD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "technique"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 30.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(8); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var loc = player.getLocation().add(0, 1, 0);
        var plugin = JavaPlugin.getPlugin(Deepwither_V2.class);

        // 高速回転・近接追従の渦斬撃 AoE + 足元衝撃波リング
        AnimeVfxHelper.spawnSpinningMeleeAura(plugin, player, 3.6, Color.fromRGB(130, 232, 255), 14, 34.0);
        AnimeVfxHelper.spawnShockwave(plugin, player.getLocation().add(0, 0.15, 0),
                0.7, 3.6, Color.fromRGB(175, 242, 255), 10, new Vector(0, 1, 0));
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 10, 1.5, 0.3, 1.5, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.35f);

        player.getNearbyEntities(3.5, 3.5, 3.5).forEach(entity -> {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                damagePipelineManager.processScaledDamage(player, living, DamageType.PHYSICAL, 1.25, getTags(), getId(), 500L);
                AnimeVfxHelper.spawnHitSlashCross(plugin,
                        living.getLocation().add(0, living.getHeight() * 0.55, 0),
                        Color.fromRGB(165, 240, 255), 2.1, 5);
            }
        });

        return CastResult.success();
    }
}
