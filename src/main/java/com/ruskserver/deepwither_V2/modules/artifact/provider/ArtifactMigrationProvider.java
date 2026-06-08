package com.ruskserver.deepwither_V2.modules.artifact.provider;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class ArtifactMigrationProvider implements PlayerDataProvider<Boolean> {

    public static final DataKey<Boolean> KEY = new DataKey<>("artifact_character_migration_done");

    @Override
    public DataKey<Boolean> getKey() {
        return KEY;
    }

    @Override
    public Boolean loadFromDb(UUID uuid, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_artifact_migrations (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "migrated BOOLEAN NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT migrated FROM player_artifact_migrations WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("migrated");
                }
            }
        }
        return false;
    }

    @Override
    public void saveToDb(UUID uuid, Boolean data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_artifact_migrations (uuid, migrated) KEY(uuid) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, Boolean.TRUE.equals(data));
            stmt.executeUpdate();
        }
    }
}
