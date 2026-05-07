package com.ruskserver.deepwither_V2.modules.trader.provider;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TraderReputationProvider implements PlayerDataProvider<TraderReputationProvider.ReputationData> {

    public static final DataKey<ReputationData> KEY = new DataKey<>("trader_reputation");
    private static final Gson gson = new Gson();

    @Override
    public DataKey<ReputationData> getKey() {
        return KEY;
    }

    @Override
    public ReputationData loadFromDb(UUID uuid, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_trader_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "reputations TEXT, " +
                        "completed_tasks INT DEFAULT 0, " +
                        "last_reset BIGINT DEFAULT 0)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT reputations, completed_tasks, last_reset FROM player_trader_data WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Integer> reputations = gson.fromJson(rs.getString("reputations"), 
                            new TypeToken<Map<String, Integer>>(){}.getType());
                    return new ReputationData(
                            reputations != null ? reputations : new HashMap<>(),
                            rs.getInt("completed_tasks"),
                            rs.getLong("last_reset")
                    );
                }
            }
        }
        return new ReputationData(new HashMap<>(), 0, System.currentTimeMillis());
    }

    @Override
    public void saveToDb(UUID uuid, ReputationData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_trader_data (uuid, reputations, completed_tasks, last_reset) KEY(uuid) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, gson.toJson(data.getReputations()));
            stmt.setInt(3, data.getCompletedTasksToday());
            stmt.setLong(4, data.getLastResetTimestamp());
            stmt.executeUpdate();
        }
    }

    public static class ReputationData {
        private final Map<String, Integer> reputations;
        private int completedTasksToday;
        private long lastResetTimestamp;

        public ReputationData(Map<String, Integer> reputations, int completedTasksToday, long lastResetTimestamp) {
            this.reputations = reputations;
            this.completedTasksToday = completedTasksToday;
            this.lastResetTimestamp = lastResetTimestamp;
        }

        public Map<String, Integer> getReputations() { return reputations; }
        public int getCompletedTasksToday() { return completedTasksToday; }
        public void setCompletedTasksToday(int count) { this.completedTasksToday = count; }
        public long getLastResetTimestamp() { return lastResetTimestamp; }
        public void setLastResetTimestamp(long timestamp) { this.lastResetTimestamp = timestamp; }
    }
}
