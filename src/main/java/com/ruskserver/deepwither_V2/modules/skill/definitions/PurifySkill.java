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
public class PurifySkill implements Skill {

    @Override
    public String getId() { return "purify"; }

    @Override
    public String getDisplayName() { return "浄化"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "聖なる輝きで穢れを払い、身体を清め力を引き出す。",
                "自身のデバフ効果を全て解除し、耐性I(4秒)を付与する。"
        );
    }

    @Override
    public Material getIcon() { return Material.MILK_BUCKET; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "support", "purify"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.DISPEL); }

    @Override
    public double getManaCost(SkillContext context) { return 15.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(12); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();
        var loc = caster.getLocation().add(0, 1, 0);

        for (PotionEffect effect : caster.getActivePotionEffects()) {
            var type = effect.getType();
            if (isHarmful(type)) {
                caster.removePotionEffect(type);
            }
        }

        caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0, false, true));

        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 20, 0.6, 0.8, 0.6, 0.05);
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 15, 0.5, 0.6, 0.5, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.3f);

        return CastResult.success();
    }

    private boolean isHarmful(PotionEffectType type) {
        return type.equals(PotionEffectType.WEAKNESS)
                || type.equals(PotionEffectType.SLOWNESS)
                || type.equals(PotionEffectType.MINING_FATIGUE)
                || type.equals(PotionEffectType.BLINDNESS)
                || type.equals(PotionEffectType.HUNGER)
                || type.equals(PotionEffectType.POISON)
                || type.equals(PotionEffectType.WITHER)
                || type.equals(PotionEffectType.LEVITATION)
                || type.equals(PotionEffectType.UNLUCK)
                || type.equals(PotionEffectType.BAD_OMEN)
                || type.equals(PotionEffectType.DARKNESS);
    }
}
