package com.ruskserver.deepwither_V2.modules.stash.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class StashInventoryHolder implements InventoryHolder {

    private final UUID playerId;
    private final UUID sessionToken;
    private final ViewType viewType;
    private final int page;
    private Inventory inventory;

    public StashInventoryHolder(UUID playerId, UUID sessionToken, ViewType viewType, int page) {
        this.playerId = playerId;
        this.sessionToken = sessionToken;
        this.viewType = viewType;
        this.page = page;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getSessionToken() {
        return sessionToken;
    }

    public ViewType getViewType() {
        return viewType;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public enum ViewType {
        STASH,
        UPGRADE_CONFIRM
    }
}
