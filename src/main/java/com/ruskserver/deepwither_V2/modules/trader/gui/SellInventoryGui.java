package com.ruskserver.deepwither_V2.modules.trader.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
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

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class SellInventoryGui implements Listener {

    private final ItemManager itemManager;
    private final TraderService traderService;
    private final ItemPDCUtil pdcUtil;
    private final DIContainer container;
    private final NamespacedKey actionKey;

    @Inject
    public SellInventoryGui(ItemManager itemManager, TraderService traderService, ItemPDCUtil pdcUtil,
                            DIContainer container, Deepwither_V2 plugin) {
        this.itemManager = itemManager;
        this.traderService = traderService;
        this.pdcUtil = pdcUtil;
        this.container = container;
        this.actionKey = new NamespacedKey(plugin, "sell_gui_action");
    }

    public void openSellGui(Player player, String npcName) {
        TraderDefinition trader = traderService.getTrader(npcName);
        if (trader == null) {
            player.sendMessage("§cトレーダー情報が見つかりません。");
            return;
        }

        SellInventoryHolder holder = new SellInventoryHolder(npcName);
        Inventory gui = Bukkit.createInventory(holder, 27, Component.text("アイテム売却 - " + trader.getDisplayName()));
        holder.setInventory(gui);

        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.empty());
            background.setItemMeta(bgMeta);
        }

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, background);
        }
        for (int i = 18; i < 27; i++) {
            gui.setItem(i, background);
        }

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("§e購入画面に戻る"));
            backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(18, backButton);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("§6売却ガイド"));
            infoMeta.lore(java.util.List.of(
                Component.text("§7中央のスロットにアイテムを置くか、"),
                Component.text("§7手持ちのアイテムを§eシフトクリック§7でまとめて売却します。")
            ));
            info.setItemMeta(infoMeta);
        }
        gui.setItem(4, info);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SellInventoryHolder holder)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory topInventory = event.getView().getTopInventory();
        ItemStack clicked = event.getCurrentItem();
        int slot = event.getRawSlot();

        if (clicked != null && clicked.hasItemMeta()) {
            String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
            if ("back".equals(action)) {
                event.setCancelled(true);
                TraderInventoryGui traderGui = container.resolve(TraderInventoryGui.class);
                traderGui.openTraderGui(player, holder.getNpcName());
                return;
            }
        }

        if (slot >= 0 && slot < 27) {
            if (slot < 9 || slot > 17) {
                event.setCancelled(true);
            }
        }

        if (event.isShiftClick() && slot >= 27) {
            if (clicked != null && clicked.getType() != Material.AIR) {
                String itemId = pdcUtil.getItemId(clicked);
                if (itemId != null) {
                    int sold = traderService.sellItem(player, itemId, clicked.getAmount());
                    if (sold > 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                    event.setCancelled(true);
                }
            }
            return;
        }

        Bukkit.getScheduler().runTask(Deepwither_V2.getPlugin(Deepwither_V2.class), () -> {
            for (int i = 9; i <= 17; i++) {
                ItemStack item = topInventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    String itemId = pdcUtil.getItemId(item);
                    if (itemId != null) {
                        if (traderService.sellItem(player, itemId)) {
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            topInventory.setItem(i, null);
                        }
                    }
                }
            }
        });
    }
}
