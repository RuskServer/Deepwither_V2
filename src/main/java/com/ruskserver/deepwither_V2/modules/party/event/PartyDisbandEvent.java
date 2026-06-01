package com.ruskserver.deepwither_V2.modules.party.event;

import com.ruskserver.deepwither_V2.modules.party.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PartyDisbandEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;
    private final Player leader;

    public PartyDisbandEvent(Party party, Player leader) {
        this.party = party;
        this.leader = leader;
    }

    public Party getParty() {
        return party;
    }

    public Player getLeader() {
        return leader;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
