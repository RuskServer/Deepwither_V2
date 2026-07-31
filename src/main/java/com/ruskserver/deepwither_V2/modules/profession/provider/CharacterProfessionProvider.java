package com.ruskserver.deepwither_V2.modules.profession.provider;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionData;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class CharacterProfessionProvider implements CharacterDataProvider<ProfessionData> {

    public static final DataKey<ProfessionData> KEY = new DataKey<>("character_professions");

    @Override
    public DataKey<ProfessionData> getKey() {
        return KEY;
    }

    @Override
    public ProfessionData loadFromDb(UUID characterId, Connection conn) throws Exception {
        ensureTable(conn);
        ProfessionData data = new ProfessionData();
        try (PreparedStatement statement = conn.prepareStatement(
                "SELECT profession_type, experience FROM character_professions WHERE character_id = ?")) {
            statement.setString(1, characterId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    try {
                        ProfessionType type = ProfessionType.valueOf(result.getString("profession_type"));
                        data.setExperience(type, result.getLong("experience"));
                    } catch (IllegalArgumentException ignored) {
                        // 廃止済みの職業行は読み飛ばす。
                    }
                }
            }
        }
        return data;
    }

    @Override
    public void saveToDb(UUID characterId, ProfessionData data, Connection conn) throws Exception {
        ensureTable(conn);
        try (PreparedStatement statement = conn.prepareStatement(
                "MERGE INTO character_professions "
                        + "(character_id, profession_type, experience) "
                        + "KEY(character_id, profession_type) VALUES (?, ?, ?)")) {
            for (ProfessionType type : ProfessionType.values()) {
                statement.setString(1, characterId.toString());
                statement.setString(2, type.name());
                statement.setLong(3, data.getExperience(type));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void ensureTable(Connection conn) throws Exception {
        try (PreparedStatement statement = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_professions ("
                        + "character_id VARCHAR(36) NOT NULL, "
                        + "profession_type VARCHAR(32) NOT NULL, "
                        + "experience BIGINT NOT NULL DEFAULT 0, "
                        + "PRIMARY KEY(character_id, profession_type))")) {
            statement.execute();
        }
    }
}
