package com.ruskserver.deepwither_V2.modules.player.provider;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CharacterAttributeProvider implements CharacterDataProvider<CharacterAttributeProvider.AttributeData> {

    public static final DataKey<AttributeData> KEY = new DataKey<>("character_attributes");

    @Override
    public DataKey<AttributeData> getKey() {
        return KEY;
    }

    @Override
    public AttributeData loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_attributes (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "points INT NOT NULL DEFAULT 0, " +
                        "str INT NOT NULL DEFAULT 0, " +
                        "vit INT NOT NULL DEFAULT 0, " +
                        "mnd INT NOT NULL DEFAULT 0, " +
                        "int_val INT NOT NULL DEFAULT 0, " +
                        "agi INT NOT NULL DEFAULT 0)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM character_attributes WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AttributeData data = new AttributeData();
                    data.setRemainingPoints(rs.getInt("points"));
                    data.setAttribute(AttributeType.STR, rs.getInt("str"));
                    data.setAttribute(AttributeType.VIT, rs.getInt("vit"));
                    data.setAttribute(AttributeType.MND, rs.getInt("mnd"));
                    data.setAttribute(AttributeType.INT, rs.getInt("int_val"));
                    data.setAttribute(AttributeType.AGI, rs.getInt("agi"));
                    return data;
                }
            }
        }
        return new AttributeData();
    }

    @Override
    public void saveToDb(UUID characterId, AttributeData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_attributes (character_id, points, str, vit, mnd, int_val, agi) " +
                        "KEY(character_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setInt(2, data.getRemainingPoints());
            stmt.setInt(3, data.getAttribute(AttributeType.STR));
            stmt.setInt(4, data.getAttribute(AttributeType.VIT));
            stmt.setInt(5, data.getAttribute(AttributeType.MND));
            stmt.setInt(6, data.getAttribute(AttributeType.INT));
            stmt.setInt(7, data.getAttribute(AttributeType.AGI));
            stmt.executeUpdate();
        }
    }

    /**
     * 残りポイントと各属性のレベルを保持するデータクラス。
     */
    public static class AttributeData {
        private int remainingPoints = 2;
        private final Map<AttributeType, Integer> attributes = new EnumMap<>(AttributeType.class);

        public AttributeData() {
            for (AttributeType type : AttributeType.values()) {
                attributes.put(type, 0);
            }
        }

        public int getRemainingPoints() {
            return remainingPoints;
        }

        public void setRemainingPoints(int remainingPoints) {
            this.remainingPoints = remainingPoints;
        }
        
        public void addRemainingPoints(int amount) {
            this.remainingPoints += amount;
        }

        public int getAttribute(AttributeType type) {
            return attributes.getOrDefault(type, 0);
        }

        public void setAttribute(AttributeType type, int level) {
            attributes.put(type, level);
        }

        public void addAttribute(AttributeType type, int amount) {
            attributes.put(type, getAttribute(type) + amount);
        }
    }
}
