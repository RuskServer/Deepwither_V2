package com.ruskserver.deepwither_V2.modules.trader.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * トレーダーUI の Inventory ベース実装。
 * 単一ページで複数商品を表示します。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class TraderInventoryGui implements Listener {

    private final ItemManager itemManager;
    private final TraderService traderService;
    private final NamespacedKey productIdKey;
    private final NamespacedKey buyPriceKey;
    private final NamespacedKey sellPriceKey;
    private final Map<Player, String> openedTraders = new HashMap<>(); // プレイヤー -> NPC名

    @Inject
    public TraderInventoryGui(ItemManager itemManager, TraderService traderService, Deepwither_V2 plugin) {
        this.itemManager = itemManager;
        this.traderService = traderService;
        this.productIdKey = new NamespacedKey(plugin, "trader_product_id");
        this.buyPriceKey = new NamespacedKey(plugin, "trader_buy_price");
        this.sellPriceKey = new NamespacedKey(plugin, "trader_sell_price");
    }

    /**
     * トレーダー UI をプレイヤーに開きます。
     */
    public void openTraderGui(Player player, String npcName) {
        TraderDefinition trader = traderService.getTrader(npcName);
        if (trader == null) {
            player.sendMessage("§cトレーダーが見つかりません。");
            return;
        }

        List<TraderProduct> products = trader.getProducts();
        int slots = Math.min(products.size(), 27); // 最大 27 スロット（3 行分）
        Inventory gui = Bukkit.createInventory(null, ((slots + 8) / 9) * 9,
                Component.text(trader.getDisplayName()));

        int slotIndex = 0;
        for (TraderProduct product : products) {
            if (slotIndex >= 27) break;
            ItemStack displayItem = createProductDisplay(product);
            gui.setItem(slotIndex, displayItem);
            slotIndex++;
        }

        player.openInventory(gui);
        openedTraders.put(player, npcName);
    }

    /**
     * トレーダー商品の表示用 ItemStack を作成します。
     */
    private ItemStack createProductDisplay(TraderProduct product) {
        com.ruskserver.deepwither_V2.modules.item.api.CustomItem customItem = itemManager.getCustomItem(product.getItemId());
        if (customItem == null) {
            return new ItemStack(Material.BARRIER);
        }

        ItemStack display = new ItemStack(customItem.getMaterial());
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            // 商品情報をPDCに保存（クリック時に識別）
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(productIdKey, PersistentDataType.STRING, product.getItemId());
            pdc.set(buyPriceKey, PersistentDataType.DOUBLE, product.getBuyPrice());
            pdc.set(sellPriceKey, PersistentDataType.DOUBLE, product.getSellPrice());

            // 表示名を設定
            meta.displayName(Component.text("§e" + customItem.getDisplayName()));

            // Lore に価格情報を表示
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("§7購入価格: §6$" + product.getBuyPrice()));
            lore.add(Component.text("§7売却価格: §6$" + product.getSellPrice()));
            lore.add(Component.text(""));
            lore.add(Component.text("§7左クリック: 購入 | 右クリック: 売却"));
            meta.lore(lore);

            display.setItemMeta(meta);
        }

        return display;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!openedTraders.containsKey(player)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String itemId = pdc.get(productIdKey, PersistentDataType.STRING);
        Double buyPrice = pdc.get(buyPriceKey, PersistentDataType.DOUBLE);
        Double sellPrice = pdc.get(sellPriceKey, PersistentDataType.DOUBLE);

        if (itemId == null || buyPrice == null || sellPrice == null) {
            return;
        }

        String npcName = openedTraders.get(player);
        TraderProduct product = new TraderProduct(itemId, buyPrice, sellPrice);

        if (event.isLeftClick()) {
            // 購入
            traderService.purchaseItem(player, product);
        } else if (event.isRightClick()) {
            // 売却
            traderService.sellItem(player, product);
        }
    }

    public void closeTrader(Player player) {
        openedTraders.remove(player);
    }
}

