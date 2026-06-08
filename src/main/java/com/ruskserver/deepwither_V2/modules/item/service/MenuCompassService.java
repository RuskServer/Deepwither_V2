package com.ruskserver.deepwither_V2.modules.item.service;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

@Service
public class MenuCompassService {
    private static final String MENU_COMPASS_ITEM_ID = "menu_compass";
    private static final String MENU_COMPASS_KEY = "menu_compass_locked";
    private static final int MENU_COMPASS_SLOT = 8;

    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;
    private final NamespacedKey menuCompassKey;

    @Inject
    public MenuCompassService(Deepwither_V2 plugin, ItemManager itemManager, ItemPDCUtil pdcUtil) {
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
        this.menuCompassKey = new NamespacedKey(plugin, MENU_COMPASS_KEY);
    }

    public void ensureMenuCompass(Player player) {
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (!isMenuCompass(item)) continue;

            if (i == MENU_COMPASS_SLOT) {
                return;
            }
            inventory.setItem(i, null);
        }

        ItemStack existing = inventory.getItem(MENU_COMPASS_SLOT);
        if (existing != null && !existing.getType().isAir()) {
            inventory.setItem(MENU_COMPASS_SLOT, null);
            var leftovers = inventory.addItem(existing);
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }

        inventory.setItem(MENU_COMPASS_SLOT, createMenuCompass());
    }

    public boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(menuCompassKey, PersistentDataType.STRING)
                || MENU_COMPASS_ITEM_ID.equals(pdcUtil.getItemId(item));
    }

    private ItemStack createMenuCompass() {
        ItemStack compass = itemManager.generate(MENU_COMPASS_ITEM_ID);
        if (compass == null) {
            compass = new ItemStack(Material.COMPASS);
            pdcUtil.setItemId(compass, MENU_COMPASS_ITEM_ID);
        }

        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(menuCompassKey, PersistentDataType.STRING, "locked");
            compass.setItemMeta(meta);
        }
        return compass;
    }
}
