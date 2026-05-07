package com.ruskserver.deepwither_V2.modules.trader.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * トレーダーシステムのコア処理。
 * - トレーダー定義の管理（NPC 名マッピング）
 * - Vault連携による取引処理
 */
@Service
public class TraderService implements Startable {

    private final Map<String, TraderDefinition> traders = new HashMap<>();
    private final DIContainer container;
    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;
    private Object economy;  // Vault Economy（Reflection で操作）
    private boolean vaultAvailable;

    @Inject
    public TraderService(DIContainer container, ItemManager itemManager, ItemPDCUtil pdcUtil) {
        this.container = container;
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
    }

    @Override
    public void start() {
        // Vault Economy の初期化
        if (!setupEconomy()) {
            Bukkit.getLogger().warning("[TraderService] Vault Economy がセットアップできませんでした。");
        }

        // DIコンテナからすべての TraderDefinition を収集して登録
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof TraderDefinition trader) {
                traders.put(trader.getNpcName(), trader);
                Bukkit.getLogger().info("[TraderService] トレーダー登録: " + trader.getNpcName());
            }
        }
    }

    /**
     * Vault Economy をセットアップします（Reflection ベース）。
     */
    private boolean setupEconomy() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                return false;
            }

            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                return false;
            }

            economy = rsp.getProvider();
            vaultAvailable = economy != null;
            return vaultAvailable;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * NPC 名に対応するトレーダーを取得します。
     */
    public TraderDefinition getTrader(String npcName) {
        return traders.get(npcName);
    }

    /**
     * プレイヤーがトレーダーからアイテムを購入します。
     * @return 成功した場合は true
     */
    public boolean purchaseItem(Player player, TraderProduct product) {
        if (!vaultAvailable || economy == null) {
            player.sendMessage("§c経済システムが利用できません。");
            return false;
        }

        try {
            double price = product.getBuyPrice();
            double balance = (double) economy.getClass().getMethod("getBalance", Player.class).invoke(economy, player);

            if (balance < price) {
                player.sendMessage("§cお金が足りません。");
                return false;
            }

            ItemStack item = itemManager.generate(product.getItemId());
            if (item == null) {
                player.sendMessage("§cアイテム取得に失敗しました。");
                return false;
            }

            // プレイヤーのインベントリにアイテムを追加
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage("§cインベントリが満杯です。");
                return false;
            }

            // お金を引き出す
            economy.getClass().getMethod("withdrawPlayer", Player.class, double.class).invoke(economy, player, price);
            player.getInventory().addItem(item);
            player.sendMessage("§a" + itemManager.getCustomItem(product.getItemId()).getDisplayName() + "§aを購入しました。");
            return true;
        } catch (Exception e) {
            player.sendMessage("§c取引処理でエラーが発生しました。");
            Bukkit.getLogger().warning("[TraderService] 購入処理エラー: " + e.getMessage());
            return false;
        }
    }

    /**
     * プレイヤーがトレーダーにアイテムを売却します。
     * @return 成功した場合は true
     */
    public boolean sellItem(Player player, String itemId) {
        if (!vaultAvailable || economy == null) {
            player.sendMessage("§c経済システムが利用できません。");
            return false;
        }

        try {
            com.ruskserver.deepwither_V2.modules.item.api.CustomItem customItem = itemManager.getCustomItem(itemId);
            if (customItem == null || customItem.getSellPrice() <= 0) {
                player.sendMessage("§cこのアイテムは売却できません。");
                return false;
            }

            // プレイヤーのインベントリから商品をもらえるアイテムを探す
            ItemStack[] inventory = player.getInventory().getContents();
            ItemStack toSell = null;
            int slotIndex = -1;

            for (int i = 0; i < inventory.length; i++) {
                ItemStack item = inventory[i];
                if (item != null) {
                    String currentItemId = pdcUtil.getItemId(item);
                    if (currentItemId != null && currentItemId.equals(itemId)) {
                        toSell = item;
                        slotIndex = i;
                        break;
                    }
                }
            }

            if (toSell == null) {
                player.sendMessage("§c売却対象のアイテムを持っていません。");
                return false;
            }

            // お金を与える
            double price = customItem.getSellPrice();
            economy.getClass().getMethod("depositPlayer", Player.class, double.class).invoke(economy, player, price);

            // アイテムを1つ減らす（スタック対応を考慮）
            if (toSell.getAmount() > 1) {
                toSell.setAmount(toSell.getAmount() - 1);
            } else {
                player.getInventory().setItem(slotIndex, null);
            }

            player.sendMessage("§a" + customItem.getDisplayName() + "§aを売却しました。");
            return true;
        } catch (Exception e) {
            player.sendMessage("§c取引処理でエラーが発生しました。");
            Bukkit.getLogger().warning("[TraderService] 売却処理エラー: " + e.getMessage());
            return false;
        }
    }
}

