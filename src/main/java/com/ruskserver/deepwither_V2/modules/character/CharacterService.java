package com.ruskserver.deepwither_V2.modules.character;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CharacterService {
    private static final int MAX_NAME_LENGTH = 32;

    private final CharacterRepository repository;
    private final Map<UUID, GameCharacter> activeCharacters = new ConcurrentHashMap<>();
    private final Map<UUID, List<GameCharacter>> characterCache = new ConcurrentHashMap<>();
    private final Set<UUID> selectionLockedPlayers = ConcurrentHashMap.newKeySet();

    @Inject
    public CharacterService(CharacterRepository repository) {
        this.repository = repository;
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
        GameCharacter character = createCharacter(ownerUuid, baseName, mode, false);
        selectCharacter(ownerUuid, character.characterId());
        return character;
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
}
