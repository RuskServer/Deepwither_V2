package com.ruskserver.deepwither_V2.modules.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class GuiRenderContext {

    private final GuiService guiService;
    private final Player player;
    private final Inventory inventory;
    private final GuiContext context;

    GuiRenderContext(GuiService guiService, Player player, Inventory inventory, GuiContext context) {
        this.guiService = guiService;
        this.player = player;
        this.inventory = inventory;
        this.context = context;
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public GuiContext context() {
        return context;
    }

    public GuiService guiService() {
        return guiService;
    }

    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }
}
