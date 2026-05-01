package com.ruskserver.deepwither_V2.modules.skill.provider;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class PlayerSkillSlotProvider implements PlayerDataProvider<PlayerSkillSlotProvider.SkillSlotData> {

    public static final DataKey<SkillSlotData> KEY = new DataKey<>("player_skill_slots");
    public static final int SLOT_COUNT = 9;

    @Override
    public DataKey<SkillSlotData> getKey() {
        return KEY;
    }

    @Override
    public SkillSlotData loadFromDb(UUID uuid, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_skill_slots (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "slot0 VARCHAR(64), slot1 VARCHAR(64), slot2 VARCHAR(64), " +
                        "slot3 VARCHAR(64), slot4 VARCHAR(64), slot5 VARCHAR(64), " +
                        "slot6 VARCHAR(64), slot7 VARCHAR(64), slot8 VARCHAR(64))")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM player_skill_slots WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SkillSlotData data = new SkillSlotData();
                    for (int i = 0; i < SLOT_COUNT; i++) {
                        data.setSkill(i, rs.getString("slot" + i));
                    }
                    return data;
                }
            }
        }
        return new SkillSlotData();
    }

    @Override
    public void saveToDb(UUID uuid, SkillSlotData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_skill_slots (uuid, slot0, slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8) " +
                        "KEY(uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            for (int i = 0; i < SLOT_COUNT; i++) {
                stmt.setString(i + 2, data.getSkill(i));
            }
            stmt.executeUpdate();
        }
    }

    public static class SkillSlotData {
        private final List<String> slots = new ArrayList<>(Collections.nCopies(SLOT_COUNT, null));

        public String getSkill(int slot) {
            if (slot < 0 || slot >= SLOT_COUNT) return null;
            return slots.get(slot);
        }

        public void setSkill(int slot, String skillId) {
            if (slot < 0 || slot >= SLOT_COUNT) return;
            slots.set(slot, skillId == null || skillId.isBlank() ? null : skillId);
        }

        public List<String> getSlots() {
            return Collections.unmodifiableList(slots);
        }

        public long getEquippedCount() {
            return slots.stream().filter(id -> id != null && !id.isBlank()).count();
        }
    }
}
