package com.ruskserver.deepwither_V2.modules.lootchest.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.lootchest.service.LootChestManager;
import com.ruskserver.deepwither_V2.modules.lootchest.service.LootRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class LootChestSetupListener implements Listener {

    private static final NamespacedKey LOOT_TABLE_KEY = new NamespacedKey("deepwither", "loot_table_id");
    private final LootChestManager manager;
    private final LootRegistry registry;

    @Inject
    public LootChestSetupListener(LootChestManager manager, LootRegistry registry) {
        this.manager = manager;
        this.registry = registry;
    }

    @EventHandler
    public void onSetup(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CHEST_MINECART) return; // スポーンエッグの代わりにチェスト付きマインカートを仮採用（または任意のアイテム）

        if (!item.hasItemMeta()) return;
        String lootTableId = item.getItemMeta().getPersistentDataContainer().get(LOOT_TABLE_KEY, PersistentDataType.STRING);
        if (lootTableId == null) return;

        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (block == null) return;

        // スニーク右クリックで削除、通常右クリックで設置
        if (event.getPlayer().isSneaking()) {
            // 削除ロジック（現時点ではマネージャーに削除メソッドを追加する必要があるが、一旦メッセージのみ）
            event.getPlayer().sendMessage("§cルートチェストの削除は未実装です。");
        } else {
            if (registry.getDefinition(lootTableId) == null) {
                event.getPlayer().sendMessage("§cエラー: ルートテーブル '" + lootTableId + "' が見つかりません。");
                return;
            }

            manager.registerNewChest(block.getLocation().add(0, 1, 0), lootTableId);
            event.getPlayer().sendMessage("§aルートチェスト (" + lootTableId + ") を設置しました！");
        }
    }
}
