package com.ruskserver.deepwither_V2.modules.character;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.database.character.CharacterData;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.character.event.CharacterSelectEvent;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterInventoryProvider;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterInventoryProvider.InventorySaveData;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterLocationProvider;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterLocationProvider.CharacterLocationData;
import com.ruskserver.deepwither_V2.modules.skill.provider.CharacterSkillSlotProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
    private final Deepwither_V2 plugin;
    private final Logger logger;
    private final Map<UUID, GameCharacter> activeCharacters = new ConcurrentHashMap<>();
    private final Map<UUID, List<GameCharacter>> characterCache = new ConcurrentHashMap<>();
    private final Set<UUID> selectionLockedPlayers = ConcurrentHashMap.newKeySet();

    @Inject
    public CharacterService(CharacterRepository repository,
                            CharacterDataRepository characterDataRepository,
                            Deepwither_V2 plugin) {
        this.repository = repository;
        this.characterDataRepository = characterDataRepository;
        this.plugin = plugin;
        this.logger = Logger.getLogger("CharacterService");
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
}


