package com.ruskserver.deepwither_V2.modules.stash;

import com.ruskserver.deepwither_V2.core.database.character.CharacterData;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.database.player.PlayerData;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import com.ruskserver.deepwither_V2.modules.stash.provider.CharacterStashDataProvider;
import com.ruskserver.deepwither_V2.modules.stash.provider.SharedStashDataProvider;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class StashService {

    public static final int PAGE_SIZE = 27;
    public static final int MAX_PAGES = 6;

    private static final double[] UPGRADE_PRICES = {
            0.0,
            0.0,
            10_000.0,
            30_000.0,
            75_000.0,
            150_000.0,
            300_000.0
    };
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##");

    private final PlayerDataRepository playerDataRepository;
    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final Logger logger;
    private final Set<UUID> upgradingOwners = ConcurrentHashMap.newKeySet();

    @Inject
    public StashService(PlayerDataRepository playerDataRepository,
                        CharacterDataRepository characterDataRepository,
                        CharacterService characterService,
                        Logger logger) {
        this.playerDataRepository = playerDataRepository;
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.logger = logger;
    }

    public Optional<StashAccess> resolveAccess(Player player) {
        Optional<GameCharacter> character = characterService.getCachedActiveCharacter(player.getUniqueId());
        if (character.isEmpty()) {
            character = characterService.getActiveCharacter(player.getUniqueId());
        }
        if (character.isEmpty()) {
            return Optional.empty();
        }

        if (character.get().mode().usesSharedProgress()) {
            PlayerData playerData = playerDataRepository.get(player.getUniqueId())
                    .orElseGet(() -> new PlayerData(player.getUniqueId()));
            StashData stashData = playerData.get(SharedStashDataProvider.KEY);
            return Optional.of(new StashAccess(
                    StashScope.SHARED,
                    player.getUniqueId(),
                    normalize(stashData)
            ));
        }

        UUID characterId = character.get().characterId();
        CharacterData characterData = characterDataRepository.get(characterId)
                .orElseGet(() -> new CharacterData(characterId));
        StashData stashData = characterData.get(CharacterStashDataProvider.KEY);
        return Optional.of(new StashAccess(
                StashScope.CHARACTER,
                characterId,
                normalize(stashData)
        ));
    }

    public ItemStack[] loadPage(StashAccess access, int page) {
        ItemStack[] contents = new ItemStack[PAGE_SIZE];
        if (!isPageUnlocked(access.data(), page)) {
            return contents;
        }

        int offset = page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            String encoded = access.data().getItems().get(offset + slot);
            if (encoded == null || encoded.isBlank()) {
                continue;
            }
            try {
                contents[slot] = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to deserialize stash item at "
                        + access.scope() + "/" + access.ownerId() + " slot " + (offset + slot), e);
            }
        }
        return contents;
    }

    public SaveResult savePage(StashAccess access, int page, Inventory inventory) {
        if (!isPageUnlocked(access.data(), page)) {
            return new SaveResult(false, access);
        }

        StashData updated = access.data().copy();
        Map<Integer, String> items = updated.getItems();
        int offset = page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int storageSlot = offset + slot;
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.isEmpty() || item.getType() == Material.AIR) {
                items.remove(storageSlot);
            } else {
                items.put(storageSlot, Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
        }

        StashAccess updatedAccess = access.withData(updated);
        return new SaveResult(save(updatedAccess), updatedAccess);
    }

    public MigrationResult migrateNativeEnderChest(Player player, StashAccess access) {
        if (access.data().isNativeEnderChestMigrated()) {
            return new MigrationResult(MigrationStatus.NOT_REQUIRED, access);
        }

        if (access.scope() == StashScope.CHARACTER) {
            StashData initialized = access.data().copy();
            initialized.setNativeEnderChestMigrated(true);
            StashAccess initializedAccess = access.withData(initialized);
            return save(initializedAccess)
                    ? new MigrationResult(MigrationStatus.INITIALIZED, initializedAccess)
                    : new MigrationResult(MigrationStatus.SAVE_FAILED, access);
        }

        Inventory nativeEnderChest = player.getEnderChest();
        StashData migrated = access.data().copy();
        int capacity = migrated.getUnlockedPages() * PAGE_SIZE;

        for (ItemStack item : nativeEnderChest.getContents()) {
            if (item == null || item.isEmpty() || item.getType() == Material.AIR) {
                continue;
            }
            int freeSlot = firstFreeSlot(migrated.getItems(), capacity);
            if (freeSlot < 0) {
                return new MigrationResult(MigrationStatus.NO_SPACE, access);
            }
            migrated.getItems().put(
                    freeSlot,
                    Base64.getEncoder().encodeToString(item.serializeAsBytes())
            );
        }

        migrated.setNativeEnderChestMigrated(true);
        StashAccess migratedAccess = access.withData(migrated);
        if (!save(migratedAccess)) {
            return new MigrationResult(MigrationStatus.SAVE_FAILED, access);
        }

        nativeEnderChest.clear();
        return new MigrationResult(MigrationStatus.MIGRATED, migratedAccess);
    }

    public UpgradeResult purchaseNextPage(Player player, StashAccess access) {
        int currentPages = access.data().getUnlockedPages();
        if (currentPages >= MAX_PAGES) {
            return new UpgradeResult(UpgradeStatus.MAX_LEVEL, access, 0.0, getBalance(player));
        }
        if (!upgradingOwners.add(access.ownerId())) {
            return new UpgradeResult(UpgradeStatus.IN_PROGRESS, access, getNextUpgradePrice(access), getBalance(player));
        }

        double price = getNextUpgradePrice(access);
        try {
            Economy economy = getEconomy();
            if (economy == null) {
                return new UpgradeResult(UpgradeStatus.ECONOMY_UNAVAILABLE, access, price, 0.0);
            }

            double balance = economy.getBalance(player);
            if (!Double.isFinite(balance) || balance < price) {
                return new UpgradeResult(UpgradeStatus.INSUFFICIENT_FUNDS, access, price, Math.max(0.0, balance));
            }

            EconomyResponse withdraw = economy.withdrawPlayer(player, price);
            if (!withdraw.transactionSuccess()) {
                logger.warning("[StashService] Upgrade withdraw failed for " + player.getUniqueId()
                        + ": " + withdraw.errorMessage);
                return new UpgradeResult(UpgradeStatus.WITHDRAW_FAILED, access, price, economy.getBalance(player));
            }

            StashData upgraded = access.data().copy();
            upgraded.setUnlockedPages(currentPages + 1);
            StashAccess upgradedAccess = access.withData(upgraded);
            if (save(upgradedAccess)) {
                return new UpgradeResult(UpgradeStatus.SUCCESS, upgradedAccess, price, economy.getBalance(player));
            }

            save(access);
            EconomyResponse refund = economy.depositPlayer(player, price);
            if (!refund.transactionSuccess()) {
                logger.severe("[StashService] Upgrade save and refund both failed for "
                        + player.getUniqueId() + ": " + refund.errorMessage);
                return new UpgradeResult(UpgradeStatus.REFUND_FAILED, access, price, economy.getBalance(player));
            }
            return new UpgradeResult(UpgradeStatus.SAVE_FAILED, access, price, economy.getBalance(player));
        } finally {
            upgradingOwners.remove(access.ownerId());
        }
    }

    public double getNextUpgradePrice(StashAccess access) {
        int targetPages = access.data().getUnlockedPages() + 1;
        if (targetPages > MAX_PAGES) {
            return 0.0;
        }
        return UPGRADE_PRICES[targetPages];
    }

    public double getBalance(Player player) {
        Economy economy = getEconomy();
        if (economy == null) {
            return 0.0;
        }
        double balance = economy.getBalance(player);
        return Double.isFinite(balance) ? Math.max(0.0, balance) : 0.0;
    }

    public String formatMoney(double amount) {
        return MONEY_FORMAT.format(Math.max(0.0, amount)) + "G";
    }

    private boolean save(StashAccess access) {
        if (access.scope() == StashScope.SHARED) {
            PlayerData playerData = playerDataRepository.get(access.ownerId())
                    .orElseGet(() -> new PlayerData(access.ownerId()));
            playerData.set(SharedStashDataProvider.KEY, access.data());
            playerDataRepository.save(access.ownerId(), playerData);
            return !playerData.getDirtyKeys().contains(SharedStashDataProvider.KEY);
        }

        CharacterData characterData = characterDataRepository.get(access.ownerId())
                .orElseGet(() -> new CharacterData(access.ownerId()));
        characterData.set(CharacterStashDataProvider.KEY, access.data());
        characterDataRepository.save(access.ownerId(), characterData);
        return !characterData.getDirtyKeys().contains(CharacterStashDataProvider.KEY);
    }

    private Economy getEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        return registration != null ? registration.getProvider() : null;
    }

    private boolean isPageUnlocked(StashData data, int page) {
        return page >= 0 && page < Math.min(data.getUnlockedPages(), MAX_PAGES);
    }

    private StashData normalize(StashData data) {
        StashData normalized = data != null ? data : new StashData();
        int clampedPages = Math.max(1, Math.min(normalized.getUnlockedPages(), MAX_PAGES));
        if (clampedPages != normalized.getUnlockedPages()) {
            normalized = normalized.copy();
            normalized.setUnlockedPages(clampedPages);
        }
        normalized.getItems();
        return normalized;
    }

    private int firstFreeSlot(Map<Integer, String> items, int capacity) {
        for (int slot = 0; slot < capacity; slot++) {
            String current = items.get(slot);
            if (current == null || current.isBlank()) {
                return slot;
            }
        }
        return -1;
    }

    public enum StashScope {
        SHARED,
        CHARACTER
    }

    public enum MigrationStatus {
        NOT_REQUIRED,
        INITIALIZED,
        MIGRATED,
        NO_SPACE,
        SAVE_FAILED
    }

    public enum UpgradeStatus {
        SUCCESS,
        MAX_LEVEL,
        IN_PROGRESS,
        ECONOMY_UNAVAILABLE,
        INSUFFICIENT_FUNDS,
        WITHDRAW_FAILED,
        SAVE_FAILED,
        REFUND_FAILED
    }

    public record StashAccess(StashScope scope, UUID ownerId, StashData data) {
        public StashAccess withData(StashData updatedData) {
            return new StashAccess(scope, ownerId, updatedData);
        }
    }

    public record SaveResult(boolean success, StashAccess access) {
    }

    public record MigrationResult(MigrationStatus status, StashAccess access) {
    }

    public record UpgradeResult(
            UpgradeStatus status,
            StashAccess access,
            double price,
            double balance
    ) {
    }
}
