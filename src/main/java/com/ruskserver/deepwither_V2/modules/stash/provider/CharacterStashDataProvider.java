package com.ruskserver.deepwither_V2.modules.stash.provider;

import com.google.gson.Gson;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.stash.StashData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class CharacterStashDataProvider implements CharacterDataProvider<StashData> {

    public static final DataKey<StashData> KEY = new DataKey<>("character_stash_data");

    private final Gson gson = new Gson();

    @Override
    public DataKey<StashData> getKey() {
        return KEY;
    }

    @Override
    public StashData loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_stashes (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "data_json TEXT NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT data_json FROM character_stashes WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    StashData loaded = gson.fromJson(rs.getString("data_json"), StashData.class);
                    return loaded != null ? loaded : new StashData();
                }
            }
        }
        return new StashData();
    }

    @Override
    public void saveToDb(UUID characterId, StashData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_stashes (character_id, data_json) KEY(character_id) VALUES (?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setString(2, gson.toJson(data != null ? data : new StashData()));
            stmt.executeUpdate();
        }
    }
}
