package com.ruskserver.deepwither_V2.modules.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class GuiInventoryHolder implements InventoryHolder {

    private final UUID playerId;
    private final String guiId;
    private final UUID sessionToken;
    private Inventory inventory;

    public GuiInventoryHolder(UUID playerId, String guiId, UUID sessionToken) {
        this.playerId = playerId;
        this.guiId = guiId;
        this.sessionToken = sessionToken;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getGuiId() {
        return guiId;
    }

    public UUID getSessionToken() {
        return sessionToken;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
