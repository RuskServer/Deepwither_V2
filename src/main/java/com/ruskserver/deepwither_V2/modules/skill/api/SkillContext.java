package com.ruskserver.deepwither_V2.modules.skill.api;

import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillCooldownService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillContext {

    private final Player caster;
    private final Skill skill;
    private final int level;
    private final ManaManager manaManager;
    private final SkillCooldownService cooldownService;

    public SkillContext(Player caster, Skill skill, int level, ManaManager manaManager, SkillCooldownService cooldownService) {
        this.caster = caster;
        this.skill = skill;
        this.level = level;
        this.manaManager = manaManager;
        this.cooldownService = cooldownService;
    }

    public Player getCaster() {
        return caster;
    }

    public Skill getSkill() {
        return skill;
    }

    public int getLevel() {
        return level;
    }

    public ManaManager getManaManager() {
        return manaManager;
    }

    public SkillCooldownService getCooldownService() {
        return cooldownService;
    }

    public Location getEyeLocation() {
        return caster.getEyeLocation();
    }

    public Vector getDirection() {
        return caster.getEyeLocation().getDirection().normalize();
    }
}
