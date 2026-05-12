package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

@Component
public class MenuCompassListener implements Listener {

    private static final String MENU_COMPASS_KEY = "menu_compass_locked";
    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;

    @Inject
    public MenuCompassListener(ItemManager itemManager, ItemPDCUtil pdcUtil) {
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        giveMenuCompass(player);
    }

    private void giveMenuCompass(Player player) {
        // ホットバーの一番右（スロット8）にコンパスを配置
        int compassSlot = 8;
        
        // 既にコンパスがある場合は何もしない
        ItemStack existingItem = player.getInventory().getItem(compassSlot);
        if (existingItem != null && existingItem.getType() == Material.COMPASS) {
            // 既存のアイテムがロックされているか確認
            ItemMeta meta = existingItem.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc.has(getMenuCompassKey(), PersistentDataType.STRING)) {
                    return; // 既にロックされているコンパスがあるので何もしない
                }
            }
        }
        
        // 新しいコンパスアイテムを作成
        ItemStack menuCompass = createLockedMenuCompass();
        
        // ホットバーの一番右に配置
        player.getInventory().setItem(compassSlot, menuCompass);
        
        // プレイヤーに通知
        player.sendMessage("§aメニューコンパスがホットバーの右側に配置されました。");
        player.sendMessage("§e右クリックでメインメニューを開けます。");
    }

    private ItemStack createLockedMenuCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        
        // アイテム名設定
        meta.setDisplayName("§aメニューコンパス");
        
        // ロア設定
        List<String> lore = List.of(
            "§7右クリックでメインメニューを開きます。",
            "§c※移動・捨てることはできません。",
            "§7このアイテムはゲーム中ずっと保持されます。"
        );
        meta.setLore(lore);
        
        // ロック用のPDCデータを設定
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(getMenuCompassKey(), PersistentDataType.STRING, "locked");
        
        compass.setItemMeta(meta);

        // ItemAbilityListenerが識別できるようcustom_item_idを設定
        pdcUtil.setItemId(compass, "menu_compass");

        return compass;
    }

    private org.bukkit.NamespacedKey getMenuCompassKey() {
        return new org.bukkit.NamespacedKey("deepwither", MENU_COMPASS_KEY);
    }
}