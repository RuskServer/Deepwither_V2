package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@Component
public class MenuCompassProtectionListener implements Listener {

    private static final String MENU_COMPASS_KEY = "menu_compass_locked";

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.COMPASS) return;
        
        if (isMenuCompass(clickedItem)) {
            event.setCancelled(true);
            player.sendMessage("§cメニューコンパスは移動できません。");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        for (ItemStack item : event.getNewItems().values()) {
            if (item != null && item.getType() == Material.COMPASS && isMenuCompass(item)) {
                event.setCancelled(true);
                player.sendMessage("§cメニューコンパスは移動できません。");
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.COMPASS && isMenuCompass(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (droppedItem.getType() == Material.COMPASS && isMenuCompass(droppedItem)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cメニューコンパスは捨てることができません。");
        }
    }

    private boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(getMenuCompassKey(), PersistentDataType.STRING);
    }

    private org.bukkit.NamespacedKey getMenuCompassKey() {
        return new org.bukkit.NamespacedKey("deepwither", MENU_COMPASS_KEY);
    }
}