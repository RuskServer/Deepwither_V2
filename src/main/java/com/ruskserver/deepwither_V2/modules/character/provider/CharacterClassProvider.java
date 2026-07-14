package com.ruskserver.deepwither_V2.modules.character.provider;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.character.CharacterClass;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class CharacterClassProvider implements CharacterDataProvider<CharacterClass> {

    public static final DataKey<CharacterClass> KEY = new DataKey<>("character_class");

    @Override
    public DataKey<CharacterClass> getKey() {
        return KEY;
    }

    @Override
    public CharacterClass loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_class_info (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "class_name VARCHAR(32) NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT class_name FROM character_class_info WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String className = rs.getString("class_name");
                    try {
                        return CharacterClass.valueOf(className);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void saveToDb(UUID characterId, CharacterClass characterClass, Connection conn) throws Exception {
        if (characterClass == null) return;
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_class_info (character_id, class_name) KEY(character_id) VALUES (?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setString(2, characterClass.name());
            stmt.executeUpdate();
        }
    }
}
