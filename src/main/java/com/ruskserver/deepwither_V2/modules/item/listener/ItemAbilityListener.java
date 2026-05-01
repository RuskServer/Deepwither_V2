package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * プレイヤーがアイテムを手にしてクリックした際に、CustomItem の onInteract フックを呼び出すリスナー。
 */
@Component
public class ItemAbilityListener implements Listener {

    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;

    @Inject
    public ItemAbilityListener(ItemManager itemManager, ItemPDCUtil pdcUtil) {
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.isEmpty()) return;

        String customId = pdcUtil.getItemId(itemStack);
        if (customId == null) return;

        CustomItem customItem = itemManager.getCustomItem(customId);
        if (customItem != null) {
            // アイテム固有のクリックアビリティ（アクティブスキル）を発動
            customItem.onInteract(event);
        }
    }
}
