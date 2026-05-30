package com.ruskserver.deepwither_V2.modules.trader.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

@Service
public class TraderService implements Startable {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##");

    private final Map<String, TraderDefinition> traders = new HashMap<>();
    private final DIContainer container;
    private final ItemManager itemManager;
    private final TraderReputationService reputationService;
    private final ItemPDCUtil pdcUtil;
    private Economy economy;
    private boolean vaultAvailable;

    @Inject
    public TraderService(DIContainer container, ItemManager itemManager, TraderReputationService reputationService, ItemPDCUtil pdcUtil) {
        this.container = container;
        this.itemManager = itemManager;
        this.reputationService = reputationService;
        this.pdcUtil = pdcUtil;
    }

    @Override
    public void start() {
        if (!setupEconomy()) {
            Bukkit.getLogger().warning("[TraderService] Vault economy is unavailable.");
        }
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof TraderDefinition trader) {
                traders.put(trader.getNpcName(), trader);
            }
        }
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        vaultAvailable = economy != null;
        return vaultAvailable;
    }

    public TraderDefinition getTrader(String npcName) {
        return traders.get(npcName);
    }

    public Map<String, TraderDefinition> getAllTraders() {
        return java.util.Collections.unmodifiableMap(traders);
    }

    public double getBalance(Player player) {
        if (!vaultAvailable || economy == null) return 0.0;
        return economy.getBalance(player);
    }

    public String formatMoney(double amount) {
        return "$" + MONEY_FORMAT.format(Math.max(0.0, amount));
    }

    public boolean purchaseItem(Player player, String npcName, TraderProduct product) {
        if (!vaultAvailable || economy == null) {
            player.sendMessage("§c経済システムが利用できません。");
            return false;
        }

        int currentRep = reputationService.getReputation(player, npcName);
        if (currentRep < product.getRequiredReputation()) {
            player.sendMessage("§c信用度が不足しています。必要: §e" + product.getRequiredReputation() + "§c, 現在: §e" + currentRep);
            return false;
        }

        double price = product.getBuyPrice();
        double balance = economy.getBalance(player);
        if (balance < price) {
            double deficit = price - balance;
            player.sendMessage("§c所持金が不足しています。");
            player.sendMessage("§7必要: §6" + formatMoney(price) + " §7現在: §6" + formatMoney(balance) + " §7不足: §c" + formatMoney(deficit));
            return false;
        }

        ItemStack item = itemManager.generate(product.getItemId());
        if (item == null) {
            player.sendMessage("§cアイテムの生成に失敗しました。");
            return false;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cインベントリが一杯です。");
            return false;
        }

        EconomyResponse withdraw = economy.withdrawPlayer(player, price);
        if (!withdraw.transactionSuccess()) {
            player.sendMessage("§c購入に失敗しました。");
            Bukkit.getLogger().warning("[TraderService] withdraw failed: " + withdraw.errorMessage);
            return false;
        }

        player.getInventory().addItem(item);
        CustomItem customItem = itemManager.getCustomItem(product.getItemId());
        String name = customItem != null ? customItem.getDisplayName() : product.getItemId();
        double newBalance = economy.getBalance(player);
        player.sendMessage("§a購入しました: " + name + " §7(価格: §6" + formatMoney(price) + "§7, 残高: §6" + formatMoney(newBalance) + "§7)");
        return true;
    }

    public boolean sellItem(Player player, String itemId) {
        return sellItem(player, itemId, 1) > 0;
    }

    public int sellItem(Player player, String itemId, int amount) {
        if (!vaultAvailable || economy == null) {
            player.sendMessage("§c経済システムが利用できません。");
            return 0;
        }

        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (customItem == null || customItem.getSellPrice() <= 0) {
            player.sendMessage("§cこのアイテムは売却できません。");
            return 0;
        }

        ItemStack[] inventory = player.getInventory().getContents();
        ItemStack toSell = null;
        int slotIndex = -1;
        for (int i = 0; i < inventory.length; i++) {
            ItemStack item = inventory[i];
            if (item == null) continue;
            String currentItemId = pdcUtil.getItemId(item);
            if (itemId.equals(currentItemId)) {
                toSell = item;
                slotIndex = i;
                break;
            }
        }
        if (toSell == null) {
            player.sendMessage("§c指定されたアイテムがインベントリに見つかりません。");
            return 0;
        }

        int sellAmount = Math.min(amount, toSell.getAmount());
        double price = customItem.getSellPrice() * sellAmount;
        EconomyResponse deposit = economy.depositPlayer(player, price);
        if (!deposit.transactionSuccess()) {
            player.sendMessage("§c売却に失敗しました。");
            Bukkit.getLogger().warning("[TraderService] deposit failed: " + deposit.errorMessage);
            return 0;
        }

        int remaining = toSell.getAmount() - sellAmount;
        if (remaining > 0) toSell.setAmount(remaining);
        else player.getInventory().setItem(slotIndex, null);

        double newBalance = economy.getBalance(player);
        player.sendMessage("§a売却しました: §f" + customItem.getDisplayName() + " §7x" + sellAmount + " §7(+§6" + formatMoney(price) + "§7, 残高: §6" + formatMoney(newBalance) + "§7)");
        return sellAmount;
    }
}
