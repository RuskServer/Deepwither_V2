package com.ruskserver.deepwither_V2.modules.artifact.service;

import com.ruskserver.deepwither_V2.core.database.character.CharacterData;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.database.player.PlayerData;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactSaveData;
import com.ruskserver.deepwither_V2.modules.artifact.provider.ArtifactMigrationProvider;
import com.ruskserver.deepwither_V2.modules.artifact.provider.CharacterArtifactProvider;
import com.ruskserver.deepwither_V2.modules.artifact.provider.PlayerArtifactProvider;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@Service
public class ArtifactEquipmentService {

    private final CharacterService characterService;
    private final CharacterDataRepository characterDataRepository;
    private final PlayerDataRepository playerDataRepository;

    @Inject
    public ArtifactEquipmentService(CharacterService characterService,
                                    CharacterDataRepository characterDataRepository,
                                    PlayerDataRepository playerDataRepository) {
        this.characterService = characterService;
        this.characterDataRepository = characterDataRepository;
        this.playerDataRepository = playerDataRepository;
    }

    public Optional<ArtifactSaveData> getEquippedArtifacts(Player player) {
        return getActiveCharacter(player).map(character -> {
            ArtifactSaveData migrated = migrateLegacyArtifactsIfNeeded(player.getUniqueId(), character.characterId());
            if (migrated != null) {
                return migrated;
            }
            return characterDataRepository.get(character.characterId())
                    .map(data -> data.get(CharacterArtifactProvider.KEY))
                    .orElse(null);
        });
    }

    public boolean saveEquippedArtifacts(Player player, ArtifactSaveData artifactData) {
        Optional<GameCharacter> active = getActiveCharacter(player);
        if (active.isEmpty()) {
            return false;
        }

        UUID characterId = active.get().characterId();
        CharacterData data = characterDataRepository.get(characterId)
                .orElseGet(() -> new CharacterData(characterId));
        data.set(CharacterArtifactProvider.KEY, artifactData);
        characterDataRepository.save(characterId, data);
        return true;
    }

    public ArtifactSaveData migrateLegacyArtifactsIfNeeded(Player player) {
        Optional<GameCharacter> active = getActiveCharacter(player);
        return active.map(character -> migrateLegacyArtifactsIfNeeded(player.getUniqueId(), character.characterId()))
                .orElse(null);
    }

    private ArtifactSaveData migrateLegacyArtifactsIfNeeded(UUID playerId, UUID characterId) {
        CharacterData characterData = characterDataRepository.get(characterId)
                .orElseGet(() -> new CharacterData(characterId));
        ArtifactSaveData current = characterData.get(CharacterArtifactProvider.KEY);
        if (current != null && !current.isEmpty()) {
            markLegacyMigrationDone(playerId);
            return null;
        }

        if (isLegacyMigrationDone(playerId)) {
            return null;
        }

        ArtifactSaveData legacy = playerDataRepository.get(playerId)
                .map(data -> data.get(PlayerArtifactProvider.KEY))
                .orElse(null);
        if (legacy == null || legacy.isEmpty()) {
            return null;
        }

        characterData.set(CharacterArtifactProvider.KEY, legacy);
        characterDataRepository.save(characterId, characterData);
        markLegacyMigrationDone(playerId);
        return legacy;
    }

    private boolean isLegacyMigrationDone(UUID playerId) {
        return playerDataRepository.get(playerId)
                .map(data -> Boolean.TRUE.equals(data.get(ArtifactMigrationProvider.KEY)))
                .orElse(false);
    }

    private void markLegacyMigrationDone(UUID playerId) {
        PlayerData data = playerDataRepository.get(playerId)
                .orElseGet(() -> new PlayerData(playerId));
        data.set(ArtifactMigrationProvider.KEY, true);
        playerDataRepository.save(playerId, data);
    }

    private Optional<GameCharacter> getActiveCharacter(Player player) {
        return characterService.getCachedActiveCharacter(player.getUniqueId())
                .or(() -> characterService.getActiveCharacter(player.getUniqueId()));
    }
}
