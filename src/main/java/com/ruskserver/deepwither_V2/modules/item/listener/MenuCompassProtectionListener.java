package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.service.MenuCompassService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

@Component
public class MenuCompassProtectionListener implements Listener {

    private final MenuCompassService menuCompassService;

    @Inject
    public MenuCompassProtectionListener(MenuCompassService menuCompassService) {
        this.menuCompassService = menuCompassService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.COMPASS) return;
        
        if (menuCompassService.isMenuCompass(clickedItem)) {
            event.setCancelled(true);
            player.sendMessage("§cメニューコンパスは移動できません。");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        for (ItemStack item : event.getNewItems().values()) {
            if (item != null && item.getType() == Material.COMPASS && menuCompassService.isMenuCompass(item)) {
                event.setCancelled(true);
                player.sendMessage("§cメニューコンパスは移動できません。");
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.COMPASS && menuCompassService.isMenuCompass(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (droppedItem.getType() == Material.COMPASS && menuCompassService.isMenuCompass(droppedItem)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cメニューコンパスは捨てることができません。");
        }
    }
}
