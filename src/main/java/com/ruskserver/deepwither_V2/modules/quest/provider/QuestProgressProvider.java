package com.ruskserver.deepwither_V2.modules.quest.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Component
public class QuestProgressProvider implements PlayerDataProvider<QuestProgressProvider.QuestProgress> {

    public static final DataKey<QuestProgress> KEY = new DataKey<>("quest_progress");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Integer>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public DataKey<QuestProgress> getKey() {
        return KEY;
    }

    @Override
    public QuestProgress loadFromDb(UUID uuid, Connection conn) throws Exception {
        // テーブル作成
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_quests (" +
                        "uuid VARCHAR(36), " +
                        "quest_id VARCHAR(64), " +
                        "state VARCHAR(20), " +
                        "counters_json VARCHAR(1024), " +
                        "accepted_at BIGINT DEFAULT 0, " +
                        "completed_at BIGINT DEFAULT 0, " +
                        "PRIMARY KEY (uuid, quest_id))")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_quest_meta (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "last_reset_date VARCHAR(10), " +
                        "daily_completions INT DEFAULT 0)")) {
            stmt.execute();
        }

        // 旧 quest_progress からの移行チェック（存在する場合）
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'QUEST_PROGRESS'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement oldStmt = conn.prepareStatement(
                            "SELECT quest_id, state, accepted_at, last_reset_date, daily_completions FROM quest_progress WHERE uuid = ?")) {
                        oldStmt.setString(1, uuid.toString());
                        try (ResultSet oldRs = oldStmt.executeQuery()) {
                            if (oldRs.next()) {
                                String qId = oldRs.getString("quest_id");
                                String st = oldRs.getString("state");
                                long acc = oldRs.getLong("accepted_at");
                                String resetDate = oldRs.getString("last_reset_date");
                                int dailyComp = oldRs.getInt("daily_completions");

                                if (qId != null && !qId.isBlank()) {
                                    try (PreparedStatement mig = conn.prepareStatement(
                                            "MERGE INTO player_quests (uuid, quest_id, state, counters_json, accepted_at, completed_at) " +
                                                    "KEY(uuid, quest_id) VALUES (?, ?, ?, ?, ?, ?)")) {
                                        mig.setString(1, uuid.toString());
                                        mig.setString(2, qId);
                                        mig.setString(3, st);
                                        mig.setString(4, "{}");
                                        mig.setLong(5, acc);
                                        mig.setLong(6, 0);
                                        mig.executeUpdate();
                                    }
                                }

                                if (resetDate != null && !resetDate.isBlank()) {
                                    try (PreparedStatement migMeta = conn.prepareStatement(
                                            "MERGE INTO player_quest_meta (uuid, last_reset_date, daily_completions) " +
                                                    "KEY(uuid) VALUES (?, ?, ?)")) {
                                        migMeta.setString(1, uuid.toString());
                                        migMeta.setString(2, resetDate);
                                        migMeta.setInt(3, dailyComp);
                                        migMeta.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // マイグレーションエラーはスキップ
        }

        // クエスト一覧取得
        Map<String, QuestEntry> entries = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT quest_id, state, counters_json, accepted_at, completed_at FROM player_quests WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String qId = rs.getString("quest_id");
                    String stStr = rs.getString("state");
                    QuestState state = QuestState.valueOf(stStr);
                    String countersJson = rs.getString("counters_json");
                    long acceptedAt = rs.getLong("accepted_at");
                    long completedAt = rs.getLong("completed_at");

                    Map<String, Integer> counters = Map.of();
                    if (countersJson != null && !countersJson.isBlank()) {
                        try {
                            counters = MAPPER.readValue(countersJson, MAP_TYPE);
                        } catch (Exception e) {
                            counters = Map.of();
                        }
                    }
                    entries.put(qId, new QuestEntry(qId, state, counters, acceptedAt, completedAt));
                }
            }
        }

        // メタデータ取得
        String lastResetDate = "";
        int dailyCompletions = 0;
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT last_reset_date, daily_completions FROM player_quest_meta WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lastResetDate = rs.getString("last_reset_date");
                    dailyCompletions = rs.getInt("daily_completions");
                }
            }
        }

        return new QuestProgress(Map.copyOf(entries), lastResetDate != null ? lastResetDate : "", dailyCompletions);
    }

    @Override
    public void saveToDb(UUID uuid, QuestProgress data, Connection conn) throws Exception {
        if (data == null) return;

        // メタ情報保存
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_quest_meta (uuid, last_reset_date, daily_completions) " +
                        "KEY(uuid) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, data.lastResetDate());
            stmt.setInt(3, data.dailyCompletions());
            stmt.executeUpdate();
        }

        // クエスト一覧保存
        if (data.entries() != null) {
            for (QuestEntry entry : data.entries().values()) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "MERGE INTO player_quests (uuid, quest_id, state, counters_json, accepted_at, completed_at) " +
                                "KEY(uuid, quest_id) VALUES (?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, entry.questId());
                    stmt.setString(3, entry.state().name());
                    String json = entry.counters() != null ? MAPPER.writeValueAsString(entry.counters()) : "{}";
                    stmt.setString(4, json);
                    stmt.setLong(5, entry.acceptedAt());
                    stmt.setLong(6, entry.completedAt());
                    stmt.executeUpdate();
                }
            }
        }
    }

    public record QuestEntry(
            String questId,
            QuestState state,
            Map<String, Integer> counters,
            long acceptedAt,
            long completedAt
    ) {
        public int getCounter(String key) {
            return counters != null ? counters.getOrDefault(key, 0) : 0;
        }

        public QuestEntry withIncrementedCounter(String key, int amount) {
            Map<String, Integer> map = counters != null ? new HashMap<>(counters) : new HashMap<>();
            map.put(key, map.getOrDefault(key, 0) + amount);
            return new QuestEntry(questId, state, Map.copyOf(map), acceptedAt, completedAt);
        }

        public QuestEntry withState(QuestState newState) {
            long compAt = (newState == QuestState.COMPLETED || newState == QuestState.TURNED_IN)
                    ? System.currentTimeMillis() : completedAt;
            return new QuestEntry(questId, newState, counters, acceptedAt, compAt);
        }
    }

    public record QuestProgress(
            Map<String, QuestEntry> entries,
            String lastResetDate,
            int dailyCompletions
    ) {
        public static QuestProgress empty() {
            return new QuestProgress(Map.of(), "", 0);
        }

        public boolean isEmpty() {
            return entries == null || entries.isEmpty();
        }

        public QuestEntry getEntry(String questId) {
            return entries != null ? entries.get(questId) : null;
        }

        public QuestState getState(String questId) {
            QuestEntry e = getEntry(questId);
            return e != null ? e.state() : QuestState.NOT_STARTED;
        }

        public boolean isAccepted(String questId) {
            return getState(questId) == QuestState.ACCEPTED;
        }

        public boolean isCompleted(String questId) {
            QuestState s = getState(questId);
            return s == QuestState.COMPLETED || s == QuestState.TURNED_IN;
        }

        public QuestProgress withEntry(QuestEntry entry) {
            Map<String, QuestEntry> map = entries != null ? new HashMap<>(entries) : new HashMap<>();
            map.put(entry.questId(), entry);
            return new QuestProgress(Map.copyOf(map), lastResetDate, dailyCompletions);
        }

        public QuestProgress withDaily(String date, int completions) {
            return new QuestProgress(entries != null ? entries : Map.of(), date, completions);
        }

        // 後方互換用ヘルパー
        public String questId() {
            if (entries == null || entries.isEmpty()) return null;
            return entries.keySet().iterator().next();
        }

        public QuestState state() {
            String qId = questId();
            return qId != null ? getState(qId) : QuestState.NOT_STARTED;
        }

        public long acceptedAt() {
            String qId = questId();
            QuestEntry e = qId != null ? getEntry(qId) : null;
            return e != null ? e.acceptedAt() : 0L;
        }
    }
}
