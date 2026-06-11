package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MartyrdomSkill implements Skill {

    private final Map<UUID, Long> activeMartyrdom = new ConcurrentHashMap<>();

    @Override
    public String getId() { return "martyrdom"; }

    @Override
    public String getDisplayName() { return "殉教"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "聖なる誓いを立て、仲間の痛みを自らの身に引き受ける。",
                "6秒間、周囲7mの味方が受けるダメージの50%を代わりに受ける。"
        );
    }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "support", "defense", "hp_cost"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.DEFENSE, SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 10.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(20); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();
        UUID id = caster.getUniqueId();

        activeMartyrdom.put(id, System.currentTimeMillis() + 6000);

        var loc = caster.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 3.5, 0.5, 3.5, 0.1);
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 25, 3.5, 0.5, 3.5, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 0.6f);

        return CastResult.success();
    }

    public boolean isActive(UUID playerId) {
        Long expiry = activeMartyrdom.get(playerId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            activeMartyrdom.remove(playerId);
            return false;
        }
        return true;
    }

    public Map<UUID, Long> getActiveMartyrdom() {
        return activeMartyrdom;
    }
}
