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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class ExecutionerSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;
    private final VirtualHealthManager healthManager;

    @Inject
    public ExecutionerSkill(DamagePipelineManager damagePipelineManager, VirtualHealthManager healthManager) {
        this.damagePipelineManager = damagePipelineManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "executioner"; }

    @Override
    public String getDisplayName() { return "エクセキューショナー"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "目の前の敵の急所を狙い、処刑の一撃を放つ。",
                "最大6m先の敵1体に物理ダメージ(250%)を与え、HP50%未満なら1.5倍になる。"
        );
    }

    @Override
    public Material getIcon() { return Material.NETHERITE_SWORD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("melee", "warrior", "technique"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST, SkillTag.Tactic.ANTI_TANK); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 35.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(10); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        Entity raw = player.getTargetEntity(6);
        if (!(raw instanceof LivingEntity target)) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("対象がいません。", net.kyori.adventure.text.format.NamedTextColor.RED));
        }

        double maxHp = healthManager.getMaxHealth(target);
        double currentHp = healthManager.getHealth(target);
        double coefficient = 2.5;

        if (currentHp < maxHp * 0.5) {
            coefficient *= 1.5;
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.3);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.6f);
        } else {
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.9f);
        }

        damagePipelineManager.processScaledDamage(player, target, DamageType.PHYSICAL, coefficient, getTags());

        return CastResult.success();
    }
}
