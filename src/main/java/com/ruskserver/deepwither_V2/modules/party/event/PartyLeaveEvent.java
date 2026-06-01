package com.ruskserver.deepwither_V2.modules.party.event;

import com.ruskserver.deepwither_V2.modules.party.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PartyLeaveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;
    private final Player player;
    private final Reason reason;

    public enum Reason {
        LEAVE,
        KICK,
        DISBAND,
        DISCONNECT
    }

    public PartyLeaveEvent(Party party, Player player, Reason reason) {
        this.party = party;
        this.player = player;
        this.reason = reason;
    }

    public Party getParty() {
        return party;
    }

    public Player getPlayer() {
        return player;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
