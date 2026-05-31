package com.ruskserver.deepwither_V2.modules.combat.health.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * エンティティの仮想HPが変動した際に発火するカスタムイベント。
 */
public class VirtualHealthChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final double oldHealth;
    private final double newHealth;
    private final double maxHealth;

    public VirtualHealthChangeEvent(LivingEntity entity, double oldHealth, double newHealth, double maxHealth) {
        this.entity = entity;
        this.oldHealth = oldHealth;
        this.newHealth = newHealth;
        this.maxHealth = maxHealth;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public double getOldHealth() {
        return oldHealth;
    }

    public double getNewHealth() {
        return newHealth;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
