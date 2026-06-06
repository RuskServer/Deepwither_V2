package com.ruskserver.deepwither_V2.modules.player.provider;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class CharacterLevelProvider implements CharacterDataProvider<CharacterLevelProvider.LevelData> {

    public static final DataKey<LevelData> KEY = new DataKey<>("character_level_data");

    @Override
    public DataKey<LevelData> getKey() {
        return KEY;
    }

    @Override
    public LevelData loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_levels (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "level INT NOT NULL DEFAULT 1, " +
                        "exp INT NOT NULL DEFAULT 0)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT level, exp FROM character_levels WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new LevelData(rs.getInt("level"), rs.getInt("exp"));
                }
            }
        }
        return new LevelData(1, 0);
    }

    @Override
    public void saveToDb(UUID characterId, LevelData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_levels (character_id, level, exp) KEY(character_id) VALUES (?, ?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setInt(2, data.getLevel());
            stmt.setInt(3, data.getExp());
            stmt.executeUpdate();
        }
    }

    /**
     * レベルと経験値を保持するデータクラス。
     */
    public static class LevelData {
        private int level;
        private int exp;

        public LevelData(int level, int exp) {
            this.level = level;
            this.exp = exp;
        }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public int getExp() { return exp; }
        public void setExp(int exp) { this.exp = exp; }
    }
}
