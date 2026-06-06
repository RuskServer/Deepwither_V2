package com.ruskserver.deepwither_V2.modules.character;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CharacterService {
    private static final int MAX_NAME_LENGTH = 32;

    private final CharacterRepository repository;

    @Inject
    public CharacterService(CharacterRepository repository) {
        this.repository = repository;
    }

    public GameCharacter ensureLegacyCharacter(Player player) {
        return ensureLegacyCharacter(player.getUniqueId(), player.getName());
    }

    public GameCharacter ensureLegacyCharacter(UUID ownerUuid, String playerName) {
        List<GameCharacter> existing = repository.findByOwner(ownerUuid);
        if (!existing.isEmpty()) {
            Optional<GameCharacter> active = repository.findActiveCharacter(ownerUuid)
                    .filter(GameCharacter::isSelectable)
                    .or(() -> existing.stream().filter(GameCharacter::isSelectable).min(Comparator.comparing(GameCharacter::createdAt)));
            active.ifPresent(character -> repository.setActiveCharacter(ownerUuid, character.characterId()));
            return active.orElse(existing.get(0));
        }

        GameCharacter character = createCharacter(ownerUuid, playerName, CharacterMode.STANDARD, true);
        repository.setActiveCharacter(ownerUuid, character.characterId());
        return character;
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
        return character;
    }

    public GameCharacter createGeneratedCharacter(Player player, CharacterMode mode) {
        String baseName = player.getName() + "-" + (getCharacters(player.getUniqueId()).size() + 1);
        GameCharacter character = createCharacter(player.getUniqueId(), baseName, mode, false);
        selectCharacter(player, character.characterId());
        return character;
    }

    public List<GameCharacter> getCharacters(UUID ownerUuid) {
        return repository.findByOwner(ownerUuid);
    }

    public Optional<GameCharacter> getActiveCharacter(UUID ownerUuid) {
        return repository.findActiveCharacter(ownerUuid);
    }

    public boolean selectCharacter(Player player, UUID characterId) {
        Optional<GameCharacter> optional = repository.findById(characterId);
        if (optional.isEmpty()) {
            return false;
        }

        GameCharacter character = optional.get();
        if (!character.ownerUuid().equals(player.getUniqueId()) || !character.isSelectable()) {
            return false;
        }

        repository.setActiveCharacter(player.getUniqueId(), characterId);
        repository.save(character.withLastPlayedAt(System.currentTimeMillis()));
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
        UUID ownerUuid = player.getUniqueId();
        Optional<GameCharacter> active = repository.findActiveCharacter(ownerUuid);
        if (active.isEmpty()) {
            return Optional.empty();
        }

        GameCharacter character = active.get();
        if (!character.mode().isHardcore()) {
            return Optional.empty();
        }

        GameCharacter dead = character.withStatus(CharacterStatus.DEAD, System.currentTimeMillis());
        repository.save(dead);
        repository.clearActiveCharacter(ownerUuid);
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

        repository.save(character.withStatus(CharacterStatus.ALIVE, 0L));
        return true;
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
