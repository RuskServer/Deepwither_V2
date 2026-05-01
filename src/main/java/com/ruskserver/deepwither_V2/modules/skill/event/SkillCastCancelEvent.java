package com.ruskserver.deepwither_V2.modules.skill.event;

import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkillCastCancelEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player caster;
    private final Skill skill;

    public SkillCastCancelEvent(Player caster, Skill skill) {
        this.caster = caster;
        this.skill = skill;
    }

    public Player getCaster() {
        return caster;
    }

    public Skill getSkill() {
        return skill;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
