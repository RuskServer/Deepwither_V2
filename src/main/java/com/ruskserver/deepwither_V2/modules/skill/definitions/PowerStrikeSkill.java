package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class PowerStrikeSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public PowerStrikeSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "power_strike"; }

    @Override
    public String getDisplayName() { return "パワーストライク"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "目の前の敵へ踏み込み、強力な一撃を叩き込む。",
                "最大6m先の敵1体に物理ダメージ(200%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.IRON_SWORD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "technique"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 20.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(4); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        Entity raw = player.getTargetEntity(6);
        if (!(raw instanceof LivingEntity target)) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("対象がいません。", NamedTextColor.RED));
        }

        var loc = target.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 15, 0.3, 0.3, 0.3, 0.2);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);

        damagePipelineManager.processScaledDamage(player, target, DamageType.PHYSICAL, 2.0, getTags());

        return CastResult.success();
    }
}
