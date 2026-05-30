package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.trader.gui.TraderInventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Component
public class ItemUpdateListener implements Listener {

    private final ItemManager itemManager;

    @Inject
    public ItemUpdateListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // ログイン時にプレイヤーのインベントリ内の全アイテムのLoreを最新化
        updateInventory(event.getPlayer().getInventory());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        // トレーダーGUIは独自のLoreを持つためスキップ
        if (event.getInventory().getHolder() instanceof TraderInventoryHolder) return;

        // チェストなどを開いたときに、中身のアイテムのLoreを最新化
        updateInventory(event.getInventory());
    }

    private void updateInventory(Inventory inventory) {
        if (inventory == null) return;
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                // カスタムアイテムかどうかの判定を含めて、メタデータ（Lore等）を更新する
                itemManager.updateItemMeta(item);
                // ※ ItemStackはミュータブルであるため、updateItemMeta内でsetItemMetaを呼べば
                // インベントリ内のインスタンスも更新されます。
            }
        }
    }
}
