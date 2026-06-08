package com.ruskserver.deepwither_V2.modules.artifact.provider;

import com.google.gson.Gson;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactSaveData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class CharacterArtifactProvider implements CharacterDataProvider<ArtifactSaveData> {

    public static final DataKey<ArtifactSaveData> KEY = new DataKey<>("character_artifact_data");

    private final Gson gson = new Gson();

    @Override
    public DataKey<ArtifactSaveData> getKey() {
        return KEY;
    }

    @Override
    public ArtifactSaveData loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_artifacts (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "data_json TEXT NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT data_json FROM character_artifacts WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
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
    public void saveToDb(UUID characterId, ArtifactSaveData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_artifacts (character_id, data_json) KEY(character_id) VALUES (?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setString(2, gson.toJson(data));
            stmt.executeUpdate();
        }
    }
}
