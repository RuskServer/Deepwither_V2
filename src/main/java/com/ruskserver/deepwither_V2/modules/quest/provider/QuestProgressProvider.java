package com.ruskserver.deepwither_V2.modules.quest.provider;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class QuestProgressProvider implements PlayerDataProvider<QuestProgressProvider.QuestProgress> {

    public static final DataKey<QuestProgress> KEY = new DataKey<>("quest_progress");

    @Override
    public DataKey<QuestProgress> getKey() {
        return KEY;
    }

    @Override
    public QuestProgress loadFromDb(UUID uuid, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS quest_progress (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "quest_id VARCHAR(64), " +
                        "state VARCHAR(20), " +
                        "accepted_at BIGINT DEFAULT 0, " +
                        "last_reset_date VARCHAR(10))")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT quest_id, state, accepted_at, last_reset_date FROM quest_progress WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new QuestProgress(
                            rs.getString("quest_id"),
                            QuestState.valueOf(rs.getString("state")),
                            rs.getLong("accepted_at"),
                            rs.getString("last_reset_date")
                    );
                }
            }
        }
        return new QuestProgress(null, null, 0, "");
    }

    @Override
    public void saveToDb(UUID uuid, QuestProgress data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO quest_progress (uuid, quest_id, state, accepted_at, last_reset_date) " +
                        "KEY(uuid) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, data.questId());
            stmt.setString(3, data.state() != null ? data.state().name() : null);
            stmt.setLong(4, data.acceptedAt());
            stmt.setString(5, data.lastResetDate());
            stmt.executeUpdate();
        }
    }

    public record QuestProgress(String questId, QuestState state, long acceptedAt, String lastResetDate) {
        public boolean isEmpty() { return questId == null; }
    }
}
