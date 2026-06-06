package com.ruskserver.deepwither_V2.modules.character.event;

import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CharacterSelectEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final GameCharacter character;

    public CharacterSelectEvent(Player player, GameCharacter character) {
        this.player = player;
        this.character = character;
    }

    public Player getPlayer() {
        return player;
    }

    public GameCharacter getCharacter() {
        return character;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
