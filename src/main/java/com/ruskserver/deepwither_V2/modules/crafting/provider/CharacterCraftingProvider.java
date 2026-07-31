package com.ruskserver.deepwither_V2.modules.crafting.provider;

import com.google.gson.Gson;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.CraftingData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class CharacterCraftingProvider implements CharacterDataProvider<CraftingData> {

    public static final DataKey<CraftingData> KEY = new DataKey<>("character_crafting");
    private final Gson gson = new Gson();

    @Override
    public DataKey<CraftingData> getKey() {
        return KEY;
    }

    @Override
    public CraftingData loadFromDb(UUID characterId, Connection connection) throws Exception {
        ensureTable(connection);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT jobs_json FROM character_crafting WHERE character_id = ?")) {
            statement.setString(1, characterId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    CraftingData data = gson.fromJson(result.getString("jobs_json"), CraftingData.class);
                    return data != null ? data : new CraftingData();
                }
            }
        }
        return new CraftingData();
    }

    @Override
    public void saveToDb(UUID characterId, CraftingData data, Connection connection) throws Exception {
        ensureTable(connection);
        try (PreparedStatement statement = connection.prepareStatement(
                "MERGE INTO character_crafting (character_id, jobs_json) KEY(character_id) VALUES (?, ?)")) {
            statement.setString(1, characterId.toString());
            statement.setString(2, gson.toJson(data != null ? data : new CraftingData()));
            statement.executeUpdate();
        }
    }

    private void ensureTable(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_crafting ("
                        + "character_id VARCHAR(36) PRIMARY KEY, "
                        + "jobs_json TEXT NOT NULL)")) {
            statement.execute();
        }
    }
}
