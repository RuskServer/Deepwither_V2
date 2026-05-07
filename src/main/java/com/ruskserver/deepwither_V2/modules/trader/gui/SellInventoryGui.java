package com.ruskserver.deepwither_V2.modules.trader.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * 売却専用 GUI。
 * プレイヤーのアイテムを買い取ります。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class SellInventoryGui implements Listener {

    private final ItemManager itemManager;
    private final TraderService traderService;
    private final ItemPDCUtil pdcUtil;
    private final TraderInventoryGui traderGui;
    private final NamespacedKey actionKey;
    private final Map<Player, String> openedTraders = new HashMap<>(); // プレイヤー -> 元のNPC名

    @Inject
    public SellInventoryGui(ItemManager itemManager, TraderService traderService, ItemPDCUtil pdcUtil, 
                            TraderInventoryGui traderGui, Deepwither_V2 plugin) {
        this.itemManager = itemManager;
        this.traderService = traderService;
        this.pdcUtil = pdcUtil;
        this.traderGui = traderGui;
        this.actionKey = new NamespacedKey(plugin, "sell_gui_action");
    }

    /**
     * 売却 UI を開きます。
     */
    public void openSellGui(Player player, String npcName) {
        TraderDefinition trader = traderService.getTrader(npcName);
        if (trader == null) {
            player.sendMessage("§cトレーダー情報が見つかりません。");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, Component.text("アイテム売却 - " + trader.getDisplayName()));

        // 背景と装飾
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.empty());
            background.setItemMeta(bgMeta);
        }

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, background);
        }

        // 売却スロット（中央 3x1）を空ける
        for (int i = 12; i <= 14; i++) {
            gui.setItem(i, null);
        }

        // 戻るボタン
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("§e購入画面に戻る"));
            backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(18, backButton);

        // 説明用アイテム
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("§6売却ガイド"));
            infoMeta.lore(java.util.List.of(
                Component.text("§7中央のスロットにアイテムを置くか、"),
                Component.text("§7手持ちのアイテムを§eシフトクリック§7して売却します。")
            ));
            info.setItemMeta(infoMeta);
        }
        gui.setItem(4, info);

        player.openInventory(gui);
        openedTraders.put(player, npcName);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!openedTraders.containsKey(player)) return;

        Inventory topInventory = event.getView().getTopInventory();
        ItemStack clicked = event.getCurrentItem();
        int slot = event.getRawSlot();

        // アクションボタンの判定
        if (clicked != null && clicked.hasItemMeta()) {
            String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
            if ("back".equals(action)) {
                event.setCancelled(true);
                String npcName = openedTraders.get(player);
                traderGui.openTraderGui(player, npcName);
                return;
            }
        }

        // トップインベントリ内の装飾品などのクリックをキャンセル
        if (slot >= 0 && slot < 27) {
            // 売却スロット(12, 13, 14)以外はキャンセル
            if (slot < 12 || slot > 14) {
                event.setCancelled(true);
            }
        }

        // 売却処理のトリガー
        // 1. 下部インベントリからのシフトクリック
        if (event.isShiftClick() && slot >= 27) {
            if (clicked != null && clicked.getType() != Material.AIR) {
                String itemId = pdcUtil.getItemId(clicked);
                if (itemId != null) {
                    if (traderService.sellItem(player, itemId)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                    event.setCancelled(true);
                }
            }
            return;
        }

        // 2. 中央スロットへのドロップ（クリックして置く動作）
        // InventoryClickEvent の直後だとアイテムがまだインベントリにないので、次の Tick で処理するか
        // または ClickType を見て判定する。ここでは単純化のため、アイテムがスロットに置かれるタイミングで監視
        Bukkit.getScheduler().runTask(Deepwither_V2.getPlugin(Deepwither_V2.class), () -> {
            for (int i = 12; i <= 14; i++) {
                ItemStack item = topInventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    String itemId = pdcUtil.getItemId(item);
                    if (itemId != null) {
                        if (traderService.sellItem(player, itemId)) {
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            // 売却成功したらアイテムは Service 内で消費されるか、ここで消す必要がある
                            // Service 側で player.getInventory() から消しているため、
                            // GUIスロットに置かれたものは別途ハンドリングが必要
                            topInventory.setItem(i, null);
                        } else {
                            // 売却できなかったらプレイヤーに戻すか、その場に残す（今回は残す）
                        }
                    }
                }
            }
        });
    }

    public void closeSellGui(Player player) {
        openedTraders.remove(player);
    }
}
