package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class HolyNovaSkill implements Skill {

    private static final double RANGE = 6.0;
    private static final double DAMAGE_COEFFICIENT = 1.8;
    private static final double HEAL_RATIO = 0.15;

    private final DamagePipelineManager damagePipelineManager;
    private final VirtualHealthManager healthManager;

    @Inject
    public HolyNovaSkill(DamagePipelineManager damagePipelineManager, VirtualHealthManager healthManager) {
        this.damagePipelineManager = damagePipelineManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getId() {
        return "holy_nova";
    }

    @Override
    public String getDisplayName() {
        return "ホーリーノヴァ";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "自身を中心に聖なる衝撃波を放つ。",
                "周囲6mの敵に魔法ダメージ(180%)を与え、自身と周囲の味方を最大HPの15%回復する。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.NETHER_STAR;
    }

    @Override
    public SkillCategory getCategory() {
        return SkillCategory.ACTIVE;
    }

    @Override
    public SkillTargetType getTargetType() {
        return SkillTargetType.SELF;
    }

    @Override
    public Set<String> getTags() {
        return Set.of("holy", "magic", "area", "heal", "support");
    }

    @Override
    public Set<SkillTag.Role> getRoles() {
        return Set.of(SkillTag.Role.ATTACK, SkillTag.Role.SUPPORT);
    }

    @Override
    public Set<SkillTag.Scaling> getScalings() {
        return Set.of(SkillTag.Scaling.MAGICAL);
    }

    @Override
    public Set<SkillTag.Tactic> getTactics() {
        return Set.of(SkillTag.Tactic.BURST);
    }

    @Override
    public Set<SkillTag.Constraint> getConstraints() {
        return Set.of(SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD);
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 45.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(16);
    }

    @Override
    public CastResult cast(SkillContext context) {
        Player caster = context.getCaster();
        var center = caster.getLocation().add(0, 0.15, 0);

        TrailCircleHelper.spawnCircle(center, RANGE, Color.fromRGB(255, 236, 128), 16, 48);
        TrailCircleHelper.spawnCircle(
                center.clone().add(0, 0.35, 0),
                RANGE - 0.4,
                Color.fromRGB(255, 255, 220),
                12,
                40,
                new Vector(0, 1, 0),
                24
        );
        caster.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 1, 0), 24, 1.2, 0.5, 1.2, 0.04);
        caster.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.35f);

        heal(caster);
        caster.getNearbyEntities(RANGE, RANGE, RANGE).forEach(entity -> {
            if (entity instanceof Player ally) {
                heal(ally);
            } else if (entity instanceof LivingEntity enemy) {
                damagePipelineManager.processScaledDamage(
                        caster,
                        enemy,
                        DamageType.MAGIC,
                        DAMAGE_COEFFICIENT,
                        getTags(),
                        getId(),
                        500L
                );
            }
        });

        return CastResult.success();
    }

    private void heal(Player player) {
        healthManager.heal(player, healthManager.getMaxHealth(player) * HEAL_RATIO);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.4, 0), 4, 0.3, 0.3, 0.3, 0);
    }
}
