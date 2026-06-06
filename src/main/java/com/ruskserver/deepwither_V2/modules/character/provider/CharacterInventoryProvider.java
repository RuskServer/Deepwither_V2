package com.ruskserver.deepwither_V2.modules.character.provider;

import com.google.gson.Gson;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CharacterInventoryProvider implements CharacterDataProvider<CharacterInventoryProvider.InventorySaveData> {

    public static final DataKey<InventorySaveData> KEY = new DataKey<>("character_inventory_data");

    private final Gson gson = new Gson();

    public static class InventorySaveData {
        // スロットインデックス -> Base64エンコードされたItemStackバイトデータ
        private Map<Integer, String> items = new HashMap<>();

        public Map<Integer, String> getItems() {
            return items;
        }

        public void setItems(Map<Integer, String> items) {
            this.items = items;
        }
    }

    @Override
    public DataKey<InventorySaveData> getKey() {
        return KEY;
    }

    @Override
    public InventorySaveData loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_inventories (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "data_json TEXT NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT data_json FROM character_inventories WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data_json");
                    if (json != null && !json.isEmpty()) {
                        InventorySaveData loaded = gson.fromJson(json, InventorySaveData.class);
                        return loaded != null ? loaded : new InventorySaveData();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void saveToDb(UUID characterId, InventorySaveData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_inventories (character_id, data_json) KEY(character_id) VALUES (?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setString(2, gson.toJson(data));
            stmt.executeUpdate();
        }
    }
}
