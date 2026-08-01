package com.ruskserver.deepwither_V2.modules.crafting.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.durability.EquipmentDurabilityService;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RepairService implements Startable {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");
    private static final double BROKEN_SURCHARGE = 1.20;

    private final EquipmentDurabilityService durabilityService;
    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;
    private Economy economy;

    @Inject
    public RepairService(EquipmentDurabilityService durabilityService, ItemManager itemManager,
                         ItemPDCUtil pdcUtil) {
        this.durabilityService = durabilityService;
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
    }

    @Override
    public void start() {
        setupEconomy();
    }

    public List<RepairQuote> getRepairableItems(Player player) {
        List<RepairQuote> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (ItemStack item : player.getInventory().getContents()) {
            RepairQuote quote = quote(item);
            if (quote != null && seen.add(quote.instanceId())) {
                result.add(quote);
            }
        }
        return result;
    }

    public RepairQuote getQuote(Player player, UUID instanceId) {
        return quote(durabilityService.findByInstanceId(player, instanceId));
    }

    public RepairResult repair(Player player, UUID instanceId) {
        RepairQuote quote = getQuote(player, instanceId);
        if (quote == null) return RepairResult.NOT_FOUND;
        if (!ensureEconomy()) return RepairResult.ECONOMY_UNAVAILABLE;

        double balance = economy.getBalance(player);
        if (!Double.isFinite(balance) || balance < quote.price()) {
            return RepairResult.INSUFFICIENT_FUNDS;
        }
        EconomyResponse withdraw = economy.withdrawPlayer(player, quote.price());
        if (!withdraw.transactionSuccess()) return RepairResult.PAYMENT_FAILED;

        ItemStack current = durabilityService.findByInstanceId(player, instanceId);
        RepairQuote revalidated = quote(current);
        if (revalidated == null || revalidated.price() != quote.price()
                || !durabilityService.repairFully(player, current)) {
            economy.depositPlayer(player, quote.price());
            return RepairResult.TARGET_CHANGED;
        }
        return RepairResult.SUCCESS;
    }

    public double getBalance(Player player) {
        return ensureEconomy() ? Math.max(0.0, economy.getBalance(player)) : 0.0;
    }

    public String formatMoney(double amount) {
        return MONEY_FORMAT.format(Math.max(0.0, amount)) + "G";
    }

    private RepairQuote quote(ItemStack item) {
        if (item == null || !durabilityService.isDurable(item)) return null;
        int maxDurability = durabilityService.getMaxDurability(item);
        int damage = durabilityService.getDamage(item);
        boolean broken = durabilityService.isBroken(item);
        if (maxDurability <= 0 || (damage <= 0 && !broken)) return null;

        UUID instanceId = durabilityService.getOrCreateInstanceId(item);
        String itemId = pdcUtil.getItemId(item);
        CustomItem customItem = itemId == null ? null : itemManager.getCustomItem(itemId);
        if (instanceId == null || customItem == null) return null;

        double fullRepairPrice = Math.max(rarityFloor(customItem.getRarity()), customItem.getSellPrice() * 0.5);
        double damageRatio = broken ? 1.0 : Math.min(1.0, (double) damage / maxDurability);
        double price = Math.ceil(fullRepairPrice * damageRatio * (broken ? BROKEN_SURCHARGE : 1.0));
        return new RepairQuote(instanceId, item.clone(), itemId, customItem.getDisplayName(),
                damage, maxDurability, broken, Math.max(1.0, price));
    }

    private double rarityFloor(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 100.0;
            case UNCOMMON -> 250.0;
            case RARE -> 750.0;
            case EPIC -> 2_000.0;
            case LEGENDARY -> 5_000.0;
        };
    }

    private boolean ensureEconomy() {
        return economy != null || setupEconomy();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = registration == null ? null : registration.getProvider();
        return economy != null;
    }

    public record RepairQuote(UUID instanceId, ItemStack preview, String itemId, String displayName,
                              int damage, int maxDurability, boolean broken, double price) {
        public int remainingDurability() {
            return broken ? 0 : Math.max(0, maxDurability - damage);
        }
    }

    public enum RepairResult {
        SUCCESS,
        NOT_FOUND,
        INSUFFICIENT_FUNDS,
        ECONOMY_UNAVAILABLE,
        PAYMENT_FAILED,
        TARGET_CHANGED
    }
}
