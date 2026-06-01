package com.ruskserver.deepwither_V2.modules.party.event;

import com.ruskserver.deepwither_V2.modules.party.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PartyJoinEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;
    private final Player player;

    public PartyJoinEvent(Party party, Player player) {
        this.party = party;
        this.player = player;
    }

    public Party getParty() {
        return party;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
