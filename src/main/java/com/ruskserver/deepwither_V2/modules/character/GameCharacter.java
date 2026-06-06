package com.ruskserver.deepwither_V2.modules.character;

import java.util.UUID;

public record GameCharacter(
        UUID characterId,
        UUID ownerUuid,
        String name,
        CharacterMode mode,
        CharacterStatus status,
        long createdAt,
        long diedAt,
        long lastPlayedAt,
        boolean migratedFromLegacy
) {
    public boolean isSelectable() {
        return status == CharacterStatus.ALIVE;
    }

    public GameCharacter withStatus(CharacterStatus newStatus, long newDiedAt) {
        return new GameCharacter(characterId, ownerUuid, name, mode, newStatus, createdAt, newDiedAt, lastPlayedAt, migratedFromLegacy);
    }

    public GameCharacter withLastPlayedAt(long timestamp) {
        return new GameCharacter(characterId, ownerUuid, name, mode, status, createdAt, diedAt, timestamp, migratedFromLegacy);
    }
}
