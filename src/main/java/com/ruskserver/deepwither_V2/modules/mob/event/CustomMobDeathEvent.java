package com.ruskserver.deepwither_V2.modules.mob.event;

import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomMobDeathEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CustomMob mob;
    private final LivingEntity entity;
    private final Player killer;

    public CustomMobDeathEvent(CustomMob mob, LivingEntity entity, @Nullable Player killer) {
        this.mob = mob;
        this.entity = entity;
        this.killer = killer;
    }

    public CustomMob getMob() {
        return mob;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public @Nullable Player getKiller() {
        return killer;
    }

    public String getMobId() {
        return mob.getMobId();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
