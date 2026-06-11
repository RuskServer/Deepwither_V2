package com.ruskserver.deepwither_V2.modules.skilltree.provider;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CharacterSkillTreeProvider implements CharacterDataProvider<CharacterSkillTreeProvider.SkillTreeData> {

    public static final DataKey<SkillTreeData> KEY = new DataKey<>("character_skill_tree");

    @Override
    public DataKey<SkillTreeData> getKey() {
        return KEY;
    }

    @Override
    public SkillTreeData loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_skill_trees (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "skill_points INT NOT NULL DEFAULT 0, " +
                        "unlocked_nodes CLOB, " +
                        "camera_positions CLOB)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT skill_points, unlocked_nodes, camera_positions FROM character_skill_trees WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new SkillTreeData(
                            rs.getInt("skill_points"),
                            parseLevels(rs.getString("unlocked_nodes")),
                            parseCameras(rs.getString("camera_positions"))
                    );
                }
            }
        }
        return new SkillTreeData();
    }

    @Override
    public void saveToDb(UUID characterId, SkillTreeData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_skill_trees (character_id, skill_points, unlocked_nodes, camera_positions) KEY(character_id) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setInt(2, data.getSkillPoints());
            stmt.setString(3, serializeLevels(data.getUnlockedNodes()));
            stmt.setString(4, serializeCameras(data.getCameraPositions()));
            stmt.executeUpdate();
        }
    }

    private static Map<String, Integer> parseLevels(String raw) {
        Map<String, Integer> result = new HashMap<>();
        if (raw == null || raw.isBlank()) return result;
        for (String token : raw.split(";")) {
            String[] parts = token.split(":", 2);
            if (parts.length != 2) continue;
            try {
                result.put(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static String serializeLevels(Map<String, Integer> levels) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            if (builder.length() > 0) builder.append(';');
            builder.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return builder.toString();
    }

    private static Map<String, CameraPosition> parseCameras(String raw) {
        Map<String, CameraPosition> result = new HashMap<>();
        if (raw == null || raw.isBlank()) return result;
        for (String token : raw.split(";")) {
            String[] parts = token.split(":", 3);
            if (parts.length != 3) continue;
            try {
                result.put(parts[0], new CameraPosition(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static String serializeCameras(Map<String, CameraPosition> cameras) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, CameraPosition> entry : cameras.entrySet()) {
            if (builder.length() > 0) builder.append(';');
            builder.append(entry.getKey()).append(':').append(entry.getValue().x()).append(':').append(entry.getValue().y());
        }
        return builder.toString();
    }

    public record CameraPosition(int x, int y) {
    }

    public static class SkillTreeData {
        private int skillPoints;
        private final Map<String, Integer> unlockedNodes;
        private final Map<String, CameraPosition> cameraPositions;

        public SkillTreeData() {
            this(2, new HashMap<>(), new HashMap<>());
        }

        public SkillTreeData(int skillPoints, Map<String, Integer> unlockedNodes, Map<String, CameraPosition> cameraPositions) {
            this.skillPoints = skillPoints;
            this.unlockedNodes = new HashMap<>(unlockedNodes);
            this.cameraPositions = new HashMap<>(cameraPositions);
        }

        public int getSkillPoints() {
            return skillPoints;
        }

        public void setSkillPoints(int skillPoints) {
            this.skillPoints = Math.max(0, skillPoints);
        }

        public void addSkillPoints(int amount) {
            setSkillPoints(this.skillPoints + amount);
        }

        public int getNodeLevel(String nodeId) {
            return unlockedNodes.getOrDefault(nodeId, 0);
        }

        public void setNodeLevel(String nodeId, int level) {
            if (level <= 0) {
                unlockedNodes.remove(nodeId);
            } else {
                unlockedNodes.put(nodeId, level);
            }
        }

        public boolean hasNode(String nodeId) {
            return getNodeLevel(nodeId) > 0;
        }

        public Map<String, Integer> getUnlockedNodes() {
            return Collections.unmodifiableMap(unlockedNodes);
        }

        public CameraPosition getCameraPosition(String treeId) {
            return cameraPositions.getOrDefault(treeId, new CameraPosition(0, 0));
        }

        public void setCameraPosition(String treeId, int x, int y) {
            cameraPositions.put(treeId, new CameraPosition(x, y));
        }

        public Map<String, CameraPosition> getCameraPositions() {
            return Collections.unmodifiableMap(cameraPositions);
        }
    }
}
