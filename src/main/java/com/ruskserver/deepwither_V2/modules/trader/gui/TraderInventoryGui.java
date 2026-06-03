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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

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

    public void openTraderGui(Player player, String npcName) {
        TraderDefinition trader = traderService.getTrader(npcName);
        if (trader == null) {
            player.sendMessage("§cトレーダーが見つかりません。");
            return;
        }

        List<TraderProduct> products = trader.getProducts();

        TraderInventoryHolder holder = new TraderInventoryHolder(npcName);
        Inventory gui = Bukkit.createInventory(holder, 27, Component.text(trader.getDisplayName()));
        holder.setInventory(gui);

        player.openInventory(gui);

        int slotIndex = 0;
        for (TraderProduct product : products) {
            if (slotIndex >= 18) break;
            gui.setItem(slotIndex, createProductDisplay(player, npcName, product));
            slotIndex++;
        }

        int reputation = reputationService.getReputation(player, npcName);
        ItemStack repInfo = new ItemStack(Material.PAPER);
        ItemMeta repMeta = repInfo.getItemMeta();
        if (repMeta != null) {
            repMeta.displayName(Component.text("§b§l信用度: " + reputation));
            repMeta.lore(List.of(Component.text("§7商品の解放条件に使用されます。")));
            repInfo.setItemMeta(repMeta);
        }
        gui.setItem(18, repInfo);

        ItemStack balanceInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceInfo.getItemMeta();
        if (balanceMeta != null) {
            double balance = traderService.getBalance(player);
            balanceMeta.displayName(Component.text("§6§l所持金: " + traderService.formatMoney(balance)));
            balanceMeta.lore(List.of(Component.text("§7商品を購入するための現在の残高です。")));
            balanceInfo.setItemMeta(balanceMeta);
        }
        gui.setItem(19, balanceInfo);

        addDailyTaskButton(player, gui, 22, npcName);

        ItemStack sellButton = new ItemStack(Material.EMERALD);
        ItemMeta sellMeta = sellButton.getItemMeta();
        if (sellMeta != null) {
            sellMeta.displayName(Component.text("§a売却メニューを開く"));
            sellMeta.lore(List.of(Component.text("§7不要なアイテムを売却します。")));
            sellMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_sell");
            sellButton.setItemMeta(sellMeta);
        }
        gui.setItem(26, sellButton);
    }

    private void addDailyTaskButton(Player player, Inventory gui, int slot, String npcName) {
        int totalCompleted = reputationService.getTotalCompletedTasksToday(player);
        int[] progress = dailyTaskService.getActiveTaskProgress(player);
        int current = progress[0];
        int target = progress[1];
        String targetMobDisplayName = dailyTaskService.getActiveTaskMobName(player);

        ItemStack taskButton = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = taskButton.getItemMeta();
        if (meta == null) return;

        List<Component> lore = new ArrayList<>();

        meta.displayName(Component.text("デイリータスク (" + npcName + ")", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "accept_task");

        lore.add(Component.text("本日の全トレーダー合計達成数: ", NamedTextColor.GRAY)
                .append(Component.text(totalCompleted + "/" + TraderReputationService.MAX_DAILY_TASKS, NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));

        if (totalCompleted >= TraderReputationService.MAX_DAILY_TASKS && target == 0) {
            lore.add(Component.empty());
            lore.add(Component.text(">> 本日の合計タスク制限に達しました <<", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            taskButton.setType(Material.BARRIER);
        } else if (target != 0) {
            lore.add(Component.text("--- 現在の目標 ---", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text((targetMobDisplayName != null ? targetMobDisplayName : "対象") + "討伐: ", NamedTextColor.GRAY)
                    .append(Component.text(current + "/" + target, NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("目標を達成して報告してください。", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("[討伐依頼] ", NamedTextColor.GREEN)
                    .append(Component.text("現在のエリア周辺の", NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("脅威となっている生命体を討伐する。", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("クリックでタスクを受注", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        taskButton.setItemMeta(meta);
        gui.setItem(slot, taskButton);
    }

    private ItemStack createProductDisplay(Player player, String npcName, TraderProduct product) {
        ItemStack display = itemManager.generate(product.getItemId());
        if (display == null) return new ItemStack(Material.BARRIER);

        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(productIdKey, PersistentDataType.STRING, product.getItemId());
        pdc.set(buyPriceKey, PersistentDataType.DOUBLE, product.getBuyPrice());
        pdc.set(requiredRepKey, PersistentDataType.INTEGER, product.getRequiredReputation());

        int currentRep = reputationService.getReputation(player, npcName);
        double balance = traderService.getBalance(player);
        boolean repOk = currentRep >= product.getRequiredReputation();
        boolean moneyOk = balance >= product.getBuyPrice();

        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }

        lore.add(Component.text(""));
        lore.add(Component.text("§7価格: §6" + traderService.formatMoney(product.getBuyPrice())));
        if (product.getRequiredReputation() > 0) {
            lore.add(Component.text("§7必要信用度: " + (repOk ? "§a" : "§c") + product.getRequiredReputation() + " §7(現在: §f" + currentRep + "§7)"));
        }
        lore.add(Component.text("§7所持金: " + (moneyOk ? "§a" : "§c") + traderService.formatMoney(balance)));
        lore.add(Component.text(""));
        lore.add(Component.text(repOk && moneyOk ? "§a▶ クリックで購入" : "§c✕ 条件を満たしていません"));

        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TraderInventoryHolder holder)) return;
        Player player = (Player) event.getWhoClicked();

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        String npcName = holder.getNpcName();
        if ("open_sell".equals(action)) {
            sellGui.openSellGui(player, npcName);
            return;
        }
        if ("accept_task".equals(action)) {
            if (dailyTaskService.acceptDynamicTask(player, npcName)) openTraderGui(player, npcName);
            return;
        }

        String itemId = pdc.get(productIdKey, PersistentDataType.STRING);
        Double buyPrice = pdc.get(buyPriceKey, PersistentDataType.DOUBLE);
        Integer requiredRep = pdc.get(requiredRepKey, PersistentDataType.INTEGER);
        if (itemId == null || buyPrice == null) return;

        TraderProduct product = new TraderProduct(itemId, buyPrice, requiredRep != null ? requiredRep : 0);
        if (traderService.purchaseItem(player, npcName, product)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
            openTraderGui(player, npcName);
        }
    }
}
