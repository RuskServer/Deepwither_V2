package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class PowerShotSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public PowerShotSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "power_shot"; }

    @Override
    public String getDisplayName() { return "パワーショット"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "弓を引き絞り、狙いすました強力な一矢を放つ。",
                "最大30m先の敵1体に物理ダメージ(180%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.ARROW; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("ranged", "archer", "power_shot"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 15.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(4); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        Entity raw = player.getTargetEntity(30);
        if (!(raw instanceof LivingEntity target)) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("対象がいません。", NamedTextColor.RED));
        }

        var eyeLoc = player.getEyeLocation();
        var dir = eyeLoc.getDirection();

        var dust = new Particle.DustOptions(Color.fromRGB(200, 220, 255), 1.2f);
        for (int i = 0; i < 12; i++) {
            var p = eyeLoc.clone().add(dir.clone().multiply(i * 0.5));
            p.getWorld().spawnParticle(Particle.DUST, p, 3, 0.05, 0.05, 0.05, 0, dust);
        }
        eyeLoc.getWorld().spawnParticle(Particle.ENCHANT, eyeLoc.add(dir.multiply(0.5)), 12, 0.1, 0.1, 0.1, 0.3);
        eyeLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, eyeLoc, 5, 0.2, 0.2, 0.2, 0);
        eyeLoc.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.8f);

        var tLoc = target.getLocation().add(0, 1, 0);
        tLoc.getWorld().spawnParticle(Particle.CRIT, tLoc, 20, 0.3, 0.3, 0.3, 0.3);
        tLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, tLoc, 8, 0.4, 0.2, 0.4, 0);
        tLoc.getWorld().playSound(tLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);

        damagePipelineManager.processScaledDamage(player, target, DamageType.PHYSICAL, 1.8, getTags());

        return CastResult.success();
    }
}
