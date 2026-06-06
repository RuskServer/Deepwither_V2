package com.ruskserver.deepwither_V2.modules.character;

import com.ruskserver.deepwither_V2.core.database.DatabaseManager;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Repository;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository
public class CharacterRepository implements Startable {
    private final DatabaseManager db;
    private final Logger logger;

    @Inject
    public CharacterRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    @Override
    public void start() {
        try (Connection conn = db.getConnection()) {
            createTables(conn);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize character tables", e);
            throw new CharacterPersistenceException("Failed to initialize character tables", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_characters (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "owner_uuid VARCHAR(36) NOT NULL, " +
                        "name VARCHAR(32) NOT NULL, " +
                        "mode VARCHAR(32) NOT NULL, " +
                        "status VARCHAR(32) NOT NULL, " +
                        "created_at BIGINT NOT NULL, " +
                        "died_at BIGINT NOT NULL DEFAULT 0, " +
                        "last_played_at BIGINT NOT NULL DEFAULT 0, " +
                        "migrated_from_legacy BOOLEAN NOT NULL DEFAULT FALSE)")) {
            stmt.execute();
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE INDEX IF NOT EXISTS idx_player_characters_owner ON player_characters(owner_uuid)")) {
            stmt.execute();
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_active_characters (" +
                        "owner_uuid VARCHAR(36) PRIMARY KEY, " +
                        "character_id VARCHAR(36) NOT NULL)")) {
            stmt.execute();
        }
    }

    public List<GameCharacter> findByOwner(UUID ownerUuid) {
        List<GameCharacter> characters = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM player_characters WHERE owner_uuid = ? ORDER BY created_at ASC")) {
            stmt.setString(1, ownerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    characters.add(mapCharacter(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load characters for " + ownerUuid, e);
            throw new CharacterPersistenceException("Failed to load characters for " + ownerUuid, e);
        }
        characters.sort(Comparator.comparing(GameCharacter::createdAt));
        return characters;
    }

    public Optional<GameCharacter> findById(UUID characterId) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM player_characters WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCharacter(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load character " + characterId, e);
            throw new CharacterPersistenceException("Failed to load character " + characterId, e);
        }
        return Optional.empty();
    }

    public Optional<UUID> findActiveCharacterId(UUID ownerUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT character_id FROM player_active_characters WHERE owner_uuid = ?")) {
            stmt.setString(1, ownerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    try {
                        return Optional.of(UUID.fromString(rs.getString("character_id")));
                    } catch (IllegalArgumentException e) {
                        throw new SQLException("Invalid active character UUID: " + rs.getString("character_id"), e);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load active character for " + ownerUuid, e);
            throw new CharacterPersistenceException("Failed to load active character for " + ownerUuid, e);
        }
        return Optional.empty();
    }

    public Optional<GameCharacter> findActiveCharacter(UUID ownerUuid) {
        return findActiveCharacterId(ownerUuid)
                .flatMap(this::findById)
                .filter(character -> character.ownerUuid().equals(ownerUuid));
    }

    public void save(GameCharacter character) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "MERGE INTO player_characters (character_id, owner_uuid, name, mode, status, created_at, died_at, last_played_at, migrated_from_legacy) " +
                             "KEY(character_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, character.characterId().toString());
            stmt.setString(2, character.ownerUuid().toString());
            stmt.setString(3, character.name());
            stmt.setString(4, character.mode().name());
            stmt.setString(5, character.status().name());
            stmt.setLong(6, character.createdAt());
            stmt.setLong(7, character.diedAt());
            stmt.setLong(8, character.lastPlayedAt());
            stmt.setBoolean(9, character.migratedFromLegacy());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save character " + character.characterId(), e);
            throw new CharacterPersistenceException("Failed to save character " + character.characterId(), e);
        }
    }

    public void setActiveCharacter(UUID ownerUuid, UUID characterId) {
        try (Connection conn = db.getConnection()) {
            validateCharacterOwnership(conn, ownerUuid, characterId);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "MERGE INTO player_active_characters (owner_uuid, character_id) KEY(owner_uuid) VALUES (?, ?)")) {
                stmt.setString(1, ownerUuid.toString());
                stmt.setString(2, characterId.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to set active character for " + ownerUuid, e);
            throw new CharacterPersistenceException("Failed to set active character for " + ownerUuid, e);
        }
    }

    private void validateCharacterOwnership(Connection conn, UUID ownerUuid, UUID characterId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM player_characters WHERE owner_uuid = ? AND character_id = ?")) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setString(2, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Character " + characterId + " does not belong to owner " + ownerUuid);
                }
            }
        }
    }

    public void clearActiveCharacter(UUID ownerUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM player_active_characters WHERE owner_uuid = ?")) {
            stmt.setString(1, ownerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to clear active character for " + ownerUuid, e);
            throw new CharacterPersistenceException("Failed to clear active character for " + ownerUuid, e);
        }
    }

    public void clearActiveCharacterIfMatches(UUID ownerUuid, UUID characterId) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM player_active_characters WHERE owner_uuid = ? AND character_id = ?")) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setString(2, characterId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to clear active character for " + ownerUuid, e);
            throw new CharacterPersistenceException("Failed to clear active character for " + ownerUuid, e);
        }
    }

    private GameCharacter mapCharacter(ResultSet rs) throws SQLException {
        try {
            return new GameCharacter(
                    UUID.fromString(rs.getString("character_id")),
                    UUID.fromString(rs.getString("owner_uuid")),
                    rs.getString("name"),
                    CharacterMode.valueOf(rs.getString("mode")),
                    CharacterStatus.valueOf(rs.getString("status")),
                    rs.getLong("created_at"),
                    rs.getLong("died_at"),
                    rs.getLong("last_played_at"),
                    rs.getBoolean("migrated_from_legacy")
            );
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid character row values", e);
        }
    }
}
