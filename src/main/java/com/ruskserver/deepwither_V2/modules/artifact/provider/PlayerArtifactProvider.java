package com.ruskserver.deepwither_V2.modules.artifact.provider;

import com.google.gson.Gson;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PlayerArtifactProvider implements PlayerDataProvider<PlayerArtifactProvider.ArtifactSaveData> {

    public static final DataKey<ArtifactSaveData> KEY = new DataKey<>("artifact_data");

    private final Gson gson = new Gson();

    public static class ArtifactSaveData {
        private Map<Integer, String> equippedArtifacts = new HashMap<>();

        public Map<Integer, String> getEquippedArtifacts() {
            return equippedArtifacts;
        }

        public void setEquippedArtifact(int slot, String base64) {
            if (base64 == null) {
                equippedArtifacts.remove(slot);
            } else {
                equippedArtifacts.put(slot, base64);
            }
        }
    }

    @Override
    public DataKey<ArtifactSaveData> getKey() {
        return KEY;
    }

    @Override
    public ArtifactSaveData loadFromDb(UUID uuid, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_artifacts (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "data_json TEXT NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT data_json FROM player_artifacts WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    if (json != null && !json.isEmpty()) {
                        ArtifactSaveData loaded = gson.fromJson(json, ArtifactSaveData.class);
                        return loaded != null ? loaded : new ArtifactSaveData();
                    }
                }
            }
        }
        return new ArtifactSaveData();
    }

    @Override
    public void saveToDb(UUID uuid, ArtifactSaveData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_artifacts (uuid, data_json) KEY(uuid) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, gson.toJson(data));
            stmt.executeUpdate();
        }
    }
}
