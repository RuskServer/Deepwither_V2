package com.ruskserver.deepwither_V2.modules.character;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.database.character.CharacterData;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.database.player.PlayerData;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.character.event.CharacterSelectEvent;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterEconomyProvider;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterInventoryProvider;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterInventoryProvider.InventorySaveData;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterLocationProvider;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterLocationProvider.CharacterLocationData;
import com.ruskserver.deepwither_V2.modules.character.provider.SharedEconomyProvider;
import com.ruskserver.deepwither_V2.modules.skill.provider.CharacterSkillSlotProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class CharacterService {
    private static final int MAX_NAME_LENGTH = 32;

    private final CharacterRepository repository;
    private final CharacterDataRepository characterDataRepository;
    private final PlayerDataRepository playerDataRepository;
    private final Deepwither_V2 plugin;
    private final Logger logger;
    private final Map<UUID, GameCharacter> activeCharacters = new ConcurrentHashMap<>();
    private final Map<UUID, List<GameCharacter>> characterCache = new ConcurrentHashMap<>();
    private final Set<UUID> selectionLockedPlayers = ConcurrentHashMap.newKeySet();
    /**
     * 真HCキャラ切替時に、標準/SHCキャラの共有残高を一時保存する。
     * キー = プレイヤーUUID, 値 = Vault残高（切り替え前の共有残高）
     */
    private final Map<UUID, Double> sharedBalanceCache = new ConcurrentHashMap<>();
    private Economy economy;
    private boolean vaultAvailable;

    @Inject
    public CharacterService(CharacterRepository repository,
                            CharacterDataRepository characterDataRepository,
                            PlayerDataRepository playerDataRepository,
                            Deepwither_V2 plugin) {
        this.repository = repository;
        this.characterDataRepository = characterDataRepository;
        this.playerDataRepository = playerDataRepository;
        this.plugin = plugin;
        this.logger = Logger.getLogger("CharacterService");
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        economy = rsp.getProvider();
        vaultAvailable = economy != null;
        if (!vaultAvailable) {
            logger.warning("[CharacterService] Vault economy is unavailable.");
        }
    }

    /**
     * Vault経済が未初期化の場合に再試行します（コンストラクタ時点ではVaultが未準備の可能性あり）。
     */
    private void ensureEconomy() {
        if (!vaultAvailable) {
            setupEconomy();
        }
    }

    public Optional<GameCharacter> ensureLegacyCharacter(Player player) {
        return ensureLegacyCharacter(player.getUniqueId(), player.getName());
    }

    public Optional<GameCharacter> ensureLegacyCharacter(UUID ownerUuid, String playerName) {
        List<GameCharacter> existing = repository.findByOwner(ownerUuid);
        if (!existing.isEmpty()) {
            cacheCharacters(ownerUuid, existing);
            Optional<GameCharacter> active = repository.findActiveCharacter(ownerUuid).filter(GameCharacter::isSelectable);
            active.ifPresentOrElse(
                    character -> activeCharacters.put(ownerUuid, character),
                    () -> activeCharacters.remove(ownerUuid)
            );
            return active;
        }

        GameCharacter character = createCharacter(ownerUuid, playerName, CharacterMode.STANDARD, true);
        repository.setActiveCharacter(ownerUuid, character.characterId());
        activeCharacters.put(ownerUuid, character);
        cacheCharacters(ownerUuid, List.of(character));
        return Optional.of(character);
    }

    public List<GameCharacter> refreshCharacterCache(UUID ownerUuid) {
        List<GameCharacter> characters = repository.findByOwner(ownerUuid);
        cacheCharacters(ownerUuid, characters);
        Optional<GameCharacter> active = repository.findActiveCharacter(ownerUuid).filter(GameCharacter::isSelectable);
        active.ifPresentOrElse(
                character -> activeCharacters.put(ownerUuid, character),
                () -> activeCharacters.remove(ownerUuid)
        );
        return getCachedCharacters(ownerUuid);
    }

    public GameCharacter createCharacter(UUID ownerUuid, String name, CharacterMode mode, boolean migratedFromLegacy) {
        long now = System.currentTimeMillis();
        GameCharacter character = new GameCharacter(
                UUID.randomUUID(),
                ownerUuid,
                normalizeName(name, mode),
                mode,
                CharacterStatus.ALIVE,
                now,
                0L,
                now,
                migratedFromLegacy
        );
        repository.save(character);
        addCachedCharacter(ownerUuid, character);
        return character;
    }

    public GameCharacter createAndSelectCharacter(UUID ownerUuid, String name, CharacterMode mode, boolean migratedFromLegacy) {
        if (isSelectionLocked(ownerUuid)) {
            throw new CharacterPersistenceException("Character selection is locked for " + ownerUuid, null);
        }

        long now = System.currentTimeMillis();
        GameCharacter character = new GameCharacter(
                UUID.randomUUID(),
                ownerUuid,
                normalizeName(name, mode),
                mode,
                CharacterStatus.ALIVE,
                now,
                0L,
                now,
                migratedFromLegacy
        );
        repository.saveAndSetActiveCharacter(character);
        if (!migratedFromLegacy) {
            initializeEmptyCharacterState(character.characterId());
        }
        activeCharacters.put(ownerUuid, character);
        addCachedCharacter(ownerUuid, character);
        return character;
    }

    public GameCharacter createGeneratedCharacter(Player player, CharacterMode mode) {
        return createGeneratedCharacter(player.getUniqueId(), player.getName(), mode);
    }

    public GameCharacter createGeneratedCharacter(UUID ownerUuid, String playerName, CharacterMode mode) {
        if (isSelectionLocked(ownerUuid)) {
            throw new CharacterPersistenceException("Character selection is locked for " + ownerUuid, null);
        }
        if (!characterCache.containsKey(ownerUuid)) {
            refreshCharacterCache(ownerUuid);
        }
        String baseName = playerName + "-" + (getCachedCharacters(ownerUuid).size() + 1);
        return createAndSelectCharacter(ownerUuid, baseName, mode, false);
    }

    public List<GameCharacter> getCharacters(UUID ownerUuid) {
        return refreshCharacterCache(ownerUuid);
    }

    public List<GameCharacter> getCachedCharacters(UUID ownerUuid) {
        return characterCache.getOrDefault(ownerUuid, List.of());
    }

    public Optional<GameCharacter> getActiveCharacter(UUID ownerUuid) {
        Optional<GameCharacter> active = repository.findActiveCharacter(ownerUuid);
        active.ifPresentOrElse(
                character -> activeCharacters.put(ownerUuid, character),
                () -> activeCharacters.remove(ownerUuid)
        );
        return active;
    }

    public Optional<GameCharacter> getCachedActiveCharacter(UUID ownerUuid) {
        return Optional.ofNullable(activeCharacters.get(ownerUuid)).filter(GameCharacter::isSelectable);
    }

    public boolean hasCachedActiveCharacter(UUID ownerUuid) {
        return getCachedActiveCharacter(ownerUuid).isPresent();
    }

    public boolean selectCharacter(Player player, UUID characterId) {
        return selectCharacter(player.getUniqueId(), characterId);
    }

    public boolean selectCharacter(UUID ownerUuid, UUID characterId) {
        if (isSelectionLocked(ownerUuid)) {
            return false;
        }

        Optional<GameCharacter> optional = repository.findById(characterId);
        if (optional.isEmpty()) {
            return false;
        }

        GameCharacter character = optional.get();
        if (!character.ownerUuid().equals(ownerUuid) || !character.isSelectable()) {
            return false;
        }

        GameCharacter selected = character.withLastPlayedAt(System.currentTimeMillis());
        repository.setActiveCharacter(ownerUuid, characterId);
        repository.save(selected);
        activeCharacters.put(ownerUuid, selected);
        replaceCachedCharacter(ownerUuid, selected);
        return true;
    }

    public Optional<GameCharacter> findOwnedCharacter(UUID ownerUuid, String token) {
        String normalized = token.toLowerCase(java.util.Locale.ROOT);
        return repository.findByOwner(ownerUuid).stream()
                .filter(character -> character.name().equalsIgnoreCase(token)
                        || character.characterId().toString().toLowerCase(java.util.Locale.ROOT).startsWith(normalized))
                .findFirst();
    }

    public Optional<GameCharacter> markActiveCharacterDead(Player player) {
        return markActiveCharacterDead(player.getUniqueId());
    }

    public Optional<GameCharacter> markActiveCharacterDead(UUID ownerUuid) {
        Optional<GameCharacter> active = repository.findActiveCharacter(ownerUuid);
        if (active.isEmpty()) {
            return Optional.empty();
        }
        return markCharacterDead(ownerUuid, active.get());
    }

    public Optional<GameCharacter> markCharacterDead(UUID ownerUuid, GameCharacter deathSnapshot) {
        if (!deathSnapshot.ownerUuid().equals(ownerUuid) || !deathSnapshot.isSelectable() || !deathSnapshot.mode().isHardcore()) {
            return Optional.empty();
        }

        GameCharacter dead = deathSnapshot.withStatus(CharacterStatus.DEAD, System.currentTimeMillis());
        repository.save(dead);
        repository.clearActiveCharacterIfMatches(ownerUuid, deathSnapshot.characterId());
        activeCharacters.remove(ownerUuid);
        replaceCachedCharacter(ownerUuid, dead);
        return Optional.of(dead);
    }

    public boolean reviveCharacter(UUID ownerUuid, String token) {
        Optional<GameCharacter> optional = findOwnedCharacter(ownerUuid, token);
        if (optional.isEmpty()) {
            return false;
        }

        GameCharacter character = optional.get();
        if (character.status() != CharacterStatus.DEAD) {
            return false;
        }

        GameCharacter revived = character.withStatus(CharacterStatus.ALIVE, 0L);
        repository.save(revived);
        replaceCachedCharacter(ownerUuid, revived);
        return true;
    }

    public boolean lockSelection(UUID ownerUuid) {
        return selectionLockedPlayers.add(ownerUuid);
    }

    public void unlockSelection(UUID ownerUuid) {
        selectionLockedPlayers.remove(ownerUuid);
    }

    public boolean isSelectionLocked(UUID ownerUuid) {
        return selectionLockedPlayers.contains(ownerUuid);
    }

    private void cacheCharacters(UUID ownerUuid, List<GameCharacter> characters) {
        characterCache.put(ownerUuid, List.copyOf(characters));
    }

    private void addCachedCharacter(UUID ownerUuid, GameCharacter character) {
        List<GameCharacter> current = new ArrayList<>(getCachedCharacters(ownerUuid));
        current.add(character);
        cacheCharacters(ownerUuid, current);
    }

    private void replaceCachedCharacter(UUID ownerUuid, GameCharacter replacement) {
        List<GameCharacter> current = new ArrayList<>(getCachedCharacters(ownerUuid));
        boolean replaced = false;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).characterId().equals(replacement.characterId())) {
                current.set(i, replacement);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            current.add(replacement);
        }
        current.sort(java.util.Comparator.comparing(GameCharacter::createdAt));
        cacheCharacters(ownerUuid, Collections.unmodifiableList(current));
    }

    private String normalizeName(String rawName, CharacterMode mode) {
        String stripped = rawName == null ? "Character" : rawName.strip();
        if (stripped.isEmpty()) {
            stripped = "Character";
        }

        String prefix = mode.getNamePrefix();
        if (!prefix.isEmpty() && !stripped.startsWith(prefix)) {
            stripped = prefix + stripped;
        }

        return stripped.length() > MAX_NAME_LENGTH ? stripped.substring(0, MAX_NAME_LENGTH) : stripped;
    }

    // =========================================================
    //  インベントリ・位置データのセーブ/ロード
    // =========================================================

    /**
     * プレイヤーの現在のインベントリと位置をアクティブキャラクターのデータとしてDBへ保存する。
     * メインスレッドから呼び出すこと（Playerオブジェクトの取得に必要）。
     */
    public void saveCharacterState(Player player) {
        getCachedActiveCharacter(player.getUniqueId()).ifPresent(character ->
                saveCharacterState(player, character.characterId()));
    }

    /**
     * プレイヤーの現在のインベントリと位置を指定キャラクターIDへDBへ保存する。
     * メインスレッドから呼び出すこと。
     */
    public void saveCharacterState(Player player, UUID characterId) {
        CharacterData data = characterDataRepository.get(characterId)
                .orElseGet(() -> new CharacterData(characterId));

        // インベントリ保存（全コンテンツをBase64シリアライズ）
        InventorySaveData invData = new InventorySaveData();
        Map<Integer, String> itemMap = new HashMap<>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                try {
                    itemMap.put(i, Base64.getEncoder().encodeToString(item.serializeAsBytes()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "インベントリスロット " + i + " のシリアライズに失敗しました", e);
                }
            }
        }
        invData.setItems(itemMap);
        data.set(CharacterInventoryProvider.KEY, invData);

        // 位置保存
        CharacterLocationData locData = CharacterLocationData.fromLocation(player.getLocation());
        if (locData != null) {
            data.set(CharacterLocationProvider.KEY, locData);
        }

        characterDataRepository.save(characterId, data);
    }

    private void initializeEmptyCharacterState(UUID characterId) {
        CharacterData data = characterDataRepository.get(characterId)
                .orElseGet(() -> new CharacterData(characterId));
        data.set(CharacterInventoryProvider.KEY, new InventorySaveData());
        data.set(CharacterSkillSlotProvider.KEY, new CharacterSkillSlotProvider.SkillSlotData());
        data.set(CharacterEconomyProvider.KEY, CharacterEconomyProvider.INITIAL_BALANCE);
        characterDataRepository.save(characterId, data);
    }

    /**
     * 指定キャラクターIDのインベントリ・位置をDBから同期的にロードし、プレイヤーへ反映する。
     * メインスレッドから呼び出すこと（DB同期呼び出しが発生するため、非同期化が望ましい場合は
     * {@link #loadAndApplyCharacterDataAsync} を使用すること）。
     */
    public void loadAndApplyCharacterData(Player player, UUID characterId) {
        CharacterData data = characterDataRepository.get(characterId)
                .orElseGet(() -> new CharacterData(characterId));
        applyCharacterDataToPlayer(player, data);
    }

    /**
     * キャラクター切り替えを非同期で行う。
     * <ol>
     *   <li>現在のキャラクターのインベントリ・位置を保存（メインスレッド）</li>
     *   <li>DBのアクティブキャラクターを更新＋新キャラデータをロード（非同期）</li>
     *   <li>インベントリ・位置をプレイヤーへ反映（メインスレッド）</li>
     *   <li>{@link CharacterSelectEvent} を発火（メインスレッド）</li>
     * </ol>
     *
     * @param player      対象プレイヤー（メインスレッドで存在すること）
     * @param characterId 選択するキャラクターID
     * @param onSuccess   切り替え成功時のコールバック（メインスレッドで実行）
     * @param onFailure   切り替え失敗時のコールバック（メインスレッドで実行）
     */
    public void switchCharacterAsync(Player player, UUID characterId,
                                     Runnable onSuccess, Runnable onFailure) {
        UUID playerId = player.getUniqueId();

        // 現在のキャラクターをセーブ（まだアクティブキャラがいる場合）
        getCachedActiveCharacter(playerId).ifPresent(prev -> {
            if (!prev.characterId().equals(characterId)) {
                saveCharacterState(player, prev.characterId());
                saveEconomyForCharacter(player, prev);
            }
        });

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean selected;
            try {
                selected = selectCharacter(playerId, characterId);
            } catch (CharacterPersistenceException e) {
                logger.log(Level.SEVERE, "キャラクター選択中にDBエラーが発生しました: " + characterId, e);
                plugin.getServer().getScheduler().runTask(plugin, onFailure);
                return;
            }

            if (!selected) {
                plugin.getServer().getScheduler().runTask(plugin, onFailure);
                return;
            }

            // 非同期スレッドでデータをロード
            CharacterData data = characterDataRepository.get(characterId)
                    .orElseGet(() -> new CharacterData(characterId));

            // メインスレッドで反映
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player online = plugin.getServer().getPlayer(playerId);
                if (online == null || !online.isOnline()) return;

                applyCharacterDataToPlayer(online, data);
                restoreEconomyForCharacter(online, characterId);

                // 選択イベント発火（SkillTreeServiceやPlayerManagerが受け取る）
                getCachedActiveCharacter(playerId).ifPresent(selected2 ->
                        plugin.getServer().getPluginManager().callEvent(
                                new CharacterSelectEvent(online, selected2)));

                onSuccess.run();
            });
        });
    }

    /**
     * 指定キャラクターIDのインベントリ・位置をDBから非同期でロードし、メインスレッドでプレイヤーへ反映する。
     * ログイン時などのメインスレッドを長時間ブロックしたくない場合に使用する。
     *
     * @param playerId    対象プレイヤーのUUID
     * @param characterId キャラクターID
     * @param afterApply  反映後に実行するコールバック（メインスレッドで実行）
     */
    public void loadAndApplyCharacterDataAsync(UUID playerId, UUID characterId, Runnable afterApply) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            CharacterData data = characterDataRepository.get(characterId)
                    .orElseGet(() -> new CharacterData(characterId));

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player online = plugin.getServer().getPlayer(playerId);
                if (online == null || !online.isOnline()) return;
                applyCharacterDataToPlayer(online, data);
                restoreEconomyForCharacter(online, characterId);
                afterApply.run();
            });
        });
    }

    /**
     * ロード済みの {@link CharacterData} をプレイヤーへ同期的に反映する内部メソッド。
     * メインスレッドから呼び出すこと。
     */
    private void applyCharacterDataToPlayer(Player player, CharacterData data) {
        // インベントリ復元
        InventorySaveData invData = data.get(CharacterInventoryProvider.KEY);
        if (invData != null) {
            player.getInventory().clear();
            ItemStack[] contents = new ItemStack[player.getInventory().getSize()];
            for (Map.Entry<Integer, String> entry : invData.getItems().entrySet()) {
                int slot = entry.getKey();
                if (slot < 0 || slot >= contents.length) continue;
                try {
                    contents[slot] = ItemStack.deserializeBytes(Base64.getDecoder().decode(entry.getValue()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "インベントリスロット " + slot + " のデシリアライズに失敗しました", e);
                }
            }
            player.getInventory().setContents(contents);
        }

        // 位置復元（データがある場合のみテレポート）
        CharacterLocationData locData = data.get(CharacterLocationProvider.KEY);
        if (locData != null) {
            Location loc = locData.toLocation();
            if (loc != null) {
                player.teleport(loc);
            }
        }
    }

    // =========================================================
    //  Vault経済のキャラクター単位同期（真HCのみ）
    // =========================================================

    /**
     * 指定キャラクターが真HCかどうかを判定します。
     */
    private boolean isTrueHardcore(GameCharacter character) {
        return character.mode() == CharacterMode.TRUE_HARDCORE;
    }

    /**
     * 真HCキャラのVault残高をDBへ保存します。
     * 標準/SHCキャラの場合は共有残高をキャッシュに退避します。
     */
    private void saveEconomyForCharacter(Player player, GameCharacter character) {
        ensureEconomy();
        if (!vaultAvailable || economy == null) return;

        double currentBalance = economy.getBalance(player);

        if (isTrueHardcore(character)) {
            // 真HC: 現在のVault残高をDBに保存
            CharacterData data = characterDataRepository.get(character.characterId())
                    .orElseGet(() -> new CharacterData(character.characterId()));
            data.set(CharacterEconomyProvider.KEY, currentBalance);
            characterDataRepository.save(character.characterId(), data);
        } else {
            // 標準/SHC: 共有残高をキャッシュ+DBに退避（クラッシュ復旧用）
            sharedBalanceCache.put(player.getUniqueId(), currentBalance);
            saveSharedBalanceToDb(player.getUniqueId(), currentBalance);
        }
    }

    /**
     * キャラクター切替時にVault残高を復元します。
     * 真HCキャラの場合はDBから読み込んだ残高をVaultに設定し、
     * 標準/SHCキャラの場合はキャッシュされた共有残高を復元します。
     */
    private void restoreEconomyForCharacter(Player player, UUID characterId) {
        ensureEconomy();
        if (!vaultAvailable || economy == null) return;

        Optional<GameCharacter> character = getCachedActiveCharacter(player.getUniqueId());
        if (character.isEmpty()) return;

        GameCharacter active = character.get();
        if (!active.characterId().equals(characterId)) return;

        if (isTrueHardcore(active)) {
            // 真HC: DBから残高を読み込み、Vaultに設定
            CharacterData data = characterDataRepository.get(characterId)
                    .orElseGet(() -> new CharacterData(characterId));
            Double savedBalance = data.get(CharacterEconomyProvider.KEY);
            double targetBalance = savedBalance != null ? savedBalance : CharacterEconomyProvider.INITIAL_BALANCE;
            setVaultBalance(player, targetBalance);
        } else {
            // 標准/SHC: キャッシュ or DBから共有残高を復元
            Double cached = sharedBalanceCache.remove(player.getUniqueId());
            if (cached == null) {
                cached = loadSharedBalanceFromDb(player.getUniqueId());
            }
            if (cached != null) {
                setVaultBalance(player, cached);
            }
        }
    }

    /**
     * Vault残高を指定金額に設定します。
     *差額をdeposit/withdrawで調整します。
     */
    private void setVaultBalance(Player player, double targetBalance) {
        if (!vaultAvailable || economy == null) return;

        double currentBalance = economy.getBalance(player);
        double diff = targetBalance - currentBalance;

        if (Math.abs(diff) < 0.001) return; // ほぼ同じなら何もしない

        if (diff > 0) {
            economy.depositPlayer(player, diff);
        } else {
            economy.withdrawPlayer(player, -diff);
        }
    }

    /**
     * 真HCキャラのVault残高を0にリセットします（死亡時の没収）。
     * DBにも0を保存します。
     */
    public void confiscateEconomy(Player player, GameCharacter character) {
        ensureEconomy();
        if (!vaultAvailable || economy == null) return;
        if (!isTrueHardcore(character)) return;

        // Vault残高を0に設定
        setVaultBalance(player, 0.0);

        // DBにも0を保存
        CharacterData data = characterDataRepository.get(character.characterId())
                .orElseGet(() -> new CharacterData(character.characterId()));
        data.set(CharacterEconomyProvider.KEY, 0.0);
        characterDataRepository.save(character.characterId(), data);
    }

    /**
     * プレイヤー退出時に共有残高キャッシュをクリーンアップします。
     */
    public void clearSharedBalanceCache(UUID playerId) {
        sharedBalanceCache.remove(playerId);
    }

    // =========================================================
    //  共有残高のDB永続化（クラッシュ復旧用）
    // =========================================================

    private void saveSharedBalanceToDb(UUID playerUuid, double balance) {
        try {
            PlayerData data = playerDataRepository.get(playerUuid)
                    .orElseGet(() -> new PlayerData(playerUuid));
            data.set(SharedEconomyProvider.KEY, balance);
            playerDataRepository.save(playerUuid, data);
        } catch (Exception e) {
            logger.log(Level.WARNING, "共有残高のDB保存に失敗しました: " + playerUuid, e);
        }
    }

    private Double loadSharedBalanceFromDb(UUID playerUuid) {
        try {
            return playerDataRepository.get(playerUuid)
                    .map(data -> data.get(SharedEconomyProvider.KEY))
                    .orElse(null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "共有残高のDB読み込みに失敗しました: " + playerUuid, e);
            return null;
        }
    }
}


