package com.ruskserver.deepwither_V2.modules.skill.event;

import com.ruskserver.deepwither_V2.modules.skill.api.SkillProjectile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkillProjectileTickEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SkillProjectile projectile;

    public SkillProjectileTickEvent(SkillProjectile projectile) {
        this.projectile = projectile;
    }

    public SkillProjectile getProjectile() {
        return projectile;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
