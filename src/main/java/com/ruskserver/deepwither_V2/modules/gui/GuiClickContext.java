package com.ruskserver.deepwither_V2.modules.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class GuiClickContext {

    private final GuiService guiService;
    private final GuiView view;
    private final GuiContext context;
    private final InventoryClickEvent event;

    GuiClickContext(GuiService guiService, GuiView view, GuiContext context, InventoryClickEvent event) {
        this.guiService = guiService;
        this.view = view;
        this.context = context;
        this.event = event;
    }

    public Player player() {
        return (Player) event.getWhoClicked();
    }

    public GuiView view() {
        return view;
    }

    public GuiContext context() {
        return context;
    }

    public InventoryClickEvent event() {
        return event;
    }

    public int slot() {
        return event.getRawSlot();
    }

    public ClickType clickType() {
        return event.getClick();
    }

    public ItemStack currentItem() {
        return event.getCurrentItem();
    }

    public void open(String guiId) {
        guiService.openLater(player(), guiId, GuiContext.EMPTY);
    }

    public void open(String guiId, GuiContext context) {
        guiService.openLater(player(), guiId, context);
    }

    public void back() {
        guiService.backLater(player());
    }

    public void close() {
        guiService.closeLater(player());
    }

    public void refresh() {
        guiService.refreshLater(player());
    }
}
