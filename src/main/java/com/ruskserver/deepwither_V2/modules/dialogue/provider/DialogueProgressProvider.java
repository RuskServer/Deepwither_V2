package com.ruskserver.deepwither_V2.modules.dialogue.provider;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Component
public class DialogueProgressProvider implements PlayerDataProvider<DialogueProgressProvider.DialogueProgress> {

    public static final DataKey<DialogueProgress> KEY = new DataKey<>("dialogue_progress");
    private static final Gson gson = new Gson();

    @Override
    public DataKey<DialogueProgress> getKey() {
        return KEY;
    }

    @Override
    public DialogueProgress loadFromDb(UUID uuid, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_dialogue_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "completed_dialogues TEXT, " +
                        "dialogue_flags TEXT)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT completed_dialogues, dialogue_flags FROM player_dialogue_data WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Set<String> completed = gson.fromJson(rs.getString("completed_dialogues"),
                            new TypeToken<Set<String>>() {}.getType());
                    Map<String, String> flags = gson.fromJson(rs.getString("dialogue_flags"),
                            new TypeToken<Map<String, String>>() {}.getType());
                    return new DialogueProgress(
                            completed != null ? completed : new HashSet<>(),
                            flags != null ? flags : new HashMap<>());
                }
            }
        }
        return new DialogueProgress(new HashSet<>(), new HashMap<>());
    }

    @Override
    public void saveToDb(UUID uuid, DialogueProgress data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_dialogue_data (uuid, completed_dialogues, dialogue_flags) KEY(uuid) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, gson.toJson(data.completedDialogues));
            stmt.setString(3, gson.toJson(data.dialogueFlags));
            stmt.executeUpdate();
        }
    }

    public static class DialogueProgress {
        private final Set<String> completedDialogues;
        private final Map<String, String> dialogueFlags;

        public DialogueProgress(Set<String> completedDialogues, Map<String, String> dialogueFlags) {
            this.completedDialogues = completedDialogues;
            this.dialogueFlags = dialogueFlags;
        }

        public Set<String> getCompletedDialogues() { return completedDialogues; }
        public Map<String, String> getDialogueFlags() { return dialogueFlags; }

        public boolean isDialogueCompleted(String dialogueId) {
            return completedDialogues.contains(dialogueId);
        }

        public void markDialogueCompleted(String dialogueId) {
            completedDialogues.add(dialogueId);
        }

        public String getFlag(String key) { return dialogueFlags.get(key); }
        public void setFlag(String key, String value) { dialogueFlags.put(key, value); }
        public boolean hasFlag(String key) { return dialogueFlags.containsKey(key); }
    }
}
