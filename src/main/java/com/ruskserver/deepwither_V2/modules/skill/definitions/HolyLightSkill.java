package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class HolyLightSkill implements Skill {

    private final VirtualHealthManager healthManager;

    @Inject
    public HolyLightSkill(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "holy_light"; }

    @Override
    public String getDisplayName() { return "聖光"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "両手に聖なる光を宿し、触れるだけで傷を癒す。",
                "対象1体の最大HPの25%を回復する。対象がない場合は自身を回復する。"
        );
    }

    @Override
    public Material getIcon() { return Material.GLOWSTONE_DUST; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "heal", "support"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 20.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(6); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();

        var ray = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                30, 1.0,
                e -> e instanceof LivingEntity && !e.equals(caster)
        );

        LivingEntity target = ray != null ? (LivingEntity) ray.getHitEntity() : null;
        if (target == null) {
            target = caster;
        }

        double maxHp = healthManager.getMaxHealth(target);
        double healAmount = maxHp * 0.25;

        double current = healthManager.getHealth(target);
        double actualHeal = Math.min(healAmount, maxHp - current);

        if (actualHeal <= 0) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("対象は既にHPが最大です。", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        }

        healthManager.heal(target, healAmount);

        var loc = target.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.4, 0.5, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.HEART, loc, 6, 0.3, 0.4, 0.3, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.5f);

        if (!target.equals(caster)) {
            caster.playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
        }

        return CastResult.success();
    }
}
