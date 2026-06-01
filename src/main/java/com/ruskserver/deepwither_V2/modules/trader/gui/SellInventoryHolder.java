package com.ruskserver.deepwither_V2.modules.trader.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SellInventoryHolder implements InventoryHolder {

    private Inventory inventory;
    private final String npcName;

    public SellInventoryHolder(String npcName) {
        this.npcName = npcName;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getNpcName() {
        return npcName;
    }
}
