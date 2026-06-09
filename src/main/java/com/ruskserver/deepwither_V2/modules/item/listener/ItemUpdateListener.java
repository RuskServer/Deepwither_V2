package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.trader.gui.TraderInventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class ItemUpdateListener implements Listener, PlayerLifecycleTask {

    private final ItemManager itemManager;

    @Inject
    public ItemUpdateListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.JOIN);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.INVENTORY_ITEMS;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> context.player().ifPresent(player -> updateInventory(player.getInventory())));
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
