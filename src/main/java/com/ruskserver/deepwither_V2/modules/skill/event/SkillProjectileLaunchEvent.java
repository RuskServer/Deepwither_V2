package com.ruskserver.deepwither_V2.modules.skill.event;

import com.ruskserver.deepwither_V2.modules.skill.api.SkillProjectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkillProjectileLaunchEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SkillProjectile projectile;
    private boolean cancelled;

    public SkillProjectileLaunchEvent(SkillProjectile projectile) {
        this.projectile = projectile;
    }

    public SkillProjectile getProjectile() {
        return projectile;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
