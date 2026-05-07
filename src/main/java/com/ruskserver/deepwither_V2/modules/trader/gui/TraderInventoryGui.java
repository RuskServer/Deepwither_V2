package com.ruskserver.deepwither_V2.modules.trader.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;
import com.ruskserver.deepwither_V2.modules.trader.service.DailyTaskService;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderReputationService;
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
    private final TraderReputationService reputationService;
    private final DailyTaskService dailyTaskService;
    private final SellInventoryGui sellGui;
    private final NamespacedKey productIdKey;
    private final NamespacedKey buyPriceKey;
    private final NamespacedKey requiredRepKey;
    private final NamespacedKey actionKey;
    private final Map<Player, String> openedTraders = new HashMap<>(); // プレイヤー -> NPC名

    @Inject
    public TraderInventoryGui(ItemManager itemManager, TraderService traderService, TraderReputationService reputationService, DailyTaskService dailyTaskService, SellInventoryGui sellGui, Deepwither_V2 plugin) {
        this.itemManager = itemManager;
        this.traderService = traderService;
        this.reputationService = reputationService;
        this.dailyTaskService = dailyTaskService;
        this.sellGui = sellGui;
        this.productIdKey = new NamespacedKey(plugin, "trader_product_id");
        this.buyPriceKey = new NamespacedKey(plugin, "trader_buy_price");
        this.requiredRepKey = new NamespacedKey(plugin, "trader_required_rep");
        this.actionKey = new NamespacedKey(plugin, "trader_gui_action");
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
        // 常に 27 スロット（3 行分）確保し、下部にボタンを配置
        Inventory gui = Bukkit.createInventory(null, 27,
                Component.text(trader.getDisplayName()));

        int slotIndex = 0;
        for (TraderProduct product : products) {
            if (slotIndex >= 18) break; // 商品は上部 2 行まで
            ItemStack displayItem = createProductDisplay(product);
            gui.setItem(slotIndex, displayItem);
            slotIndex++;
        }

        // 信用度情報の表示（左下）
        int reputation = reputationService.getReputation(player, npcName);
        ItemStack repInfo = new ItemStack(Material.PAPER);
        ItemMeta repMeta = repInfo.getItemMeta();
        if (repMeta != null) {
            repMeta.displayName(Component.text("§b§l現在の信用度: " + reputation));
            repMeta.lore(List.of(
                    Component.text("§7信用度を高めると割引などの"),
                    Component.text("§7特典が受けられます。")
            ));
            repInfo.setItemMeta(repMeta);
        }
        gui.setItem(18, repInfo);

        // デイリータスクボタン（中央下）
        int remainingSlots = reputationService.getRemainingTaskSlots(player);
        ItemStack taskButton = new ItemStack(Material.BOOK);
        ItemMeta taskMeta = taskButton.getItemMeta();
        if (taskMeta != null) {
            taskMeta.displayName(Component.text("§e§lデイリータスクを受注する"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7本日の残り回数: §f" + remainingSlots + " / 5"));
            if (dailyTaskService.hasTask(player)) {
                lore.add(Component.text(""));
                lore.add(Component.text("§6§n現在の進行状況:"));
                lore.add(Component.text("§f" + dailyTaskService.getProgressString(player)));
            } else if (remainingSlots > 0) {
                lore.add(Component.text(""));
                lore.add(Component.text("§aクリックしてタスクを受注"));
            }
            taskMeta.lore(lore);
            taskMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "accept_task");
            taskButton.setItemMeta(taskMeta);
        }
        gui.setItem(22, taskButton);

        // 売却画面への遷移ボタン（右下）
        ItemStack sellButton = new ItemStack(Material.EMERALD);
        ItemMeta sellMeta = sellButton.getItemMeta();
        if (sellMeta != null) {
            sellMeta.displayName(Component.text("§aアイテムを売却する"));
            sellMeta.lore(List.of(Component.text("§7不要なアイテムを買い取ります。")));
            sellMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_sell");
            sellButton.setItemMeta(sellMeta);
        }
        gui.setItem(26, sellButton);

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
            pdc.set(requiredRepKey, PersistentDataType.INTEGER, product.getRequiredReputation());

            // 表示名を設定
            meta.displayName(Component.text("§e" + customItem.getDisplayName()));

            // Lore に価格情報を表示
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("§7購入価格: §6$" + product.getBuyPrice()));
            if (product.getRequiredReputation() > 0) {
                lore.add(Component.text("§7必要信用度: §b" + product.getRequiredReputation()));
            }
            lore.add(Component.text(""));
            lore.add(Component.text("§eクリックして購入"));
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

        // アクション判定
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if ("open_sell".equals(action)) {
            String npcName = openedTraders.get(player);
            sellGui.openSellGui(player, npcName);
            return;
        }

        if ("accept_task".equals(action)) {
            String npcName = openedTraders.get(player);
            // 本来はランダムまたは定義済みのタスクを渡すが、一旦 ghoul_mob の討伐を固定
            if (dailyTaskService.acceptTask(player, npcName, "ghoul_mob", 5, 10)) {
                // 受注成功したらGUIを再描画して状況を反映
                openTraderGui(player, npcName);
            }
            return;
        }

        // 商品購入判定
        String itemId = pdc.get(productIdKey, PersistentDataType.STRING);
        Double buyPrice = pdc.get(buyPriceKey, PersistentDataType.DOUBLE);
        Integer requiredRep = pdc.get(requiredRepKey, PersistentDataType.INTEGER);

        if (itemId == null || buyPrice == null) {
            return;
        }

        String npcName = openedTraders.get(player);
        TraderProduct product = new TraderProduct(itemId, buyPrice, requiredRep != null ? requiredRep : 0);

        // 左クリックまたは右クリックで購入（旧仕様の右クリック売却を廃止）
        traderService.purchaseItem(player, npcName, product);
    }

    public void closeTrader(Player player) {
        openedTraders.remove(player);
    }
}

