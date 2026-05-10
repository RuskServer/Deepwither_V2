package com.ruskserver.deepwither_V2.modules.combat;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class CombatStatsProvider implements PlayerDataProvider<CombatStatsProvider.CombatStatsData> {

    public static final DataKey<CombatStatsData> KEY = new DataKey<>("combat_stats");

    @Override
    public DataKey<CombatStatsData> getKey() {
        return KEY;
    }

    @Override
    public CombatStatsData loadFromDb(UUID uuid, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_combat_stats (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "total_attacks INT, total_hits INT, total_damage DOUBLE, " +
                        "heavy_hits INT, slash_hits INT, thrust_hits INT)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM player_combat_stats WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CombatStatsData(
                            rs.getInt("total_attacks"),
                            rs.getInt("total_hits"),
                            rs.getDouble("total_damage"),
                            rs.getInt("heavy_hits"),
                            rs.getInt("slash_hits"),
                            rs.getInt("thrust_hits")
                    );
                }
            }
        }
        return new CombatStatsData();
    }

    @Override
    public void saveToDb(UUID uuid, CombatStatsData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_combat_stats (uuid, total_attacks, total_hits, total_damage, heavy_hits, slash_hits, thrust_hits) " +
                        "KEY(uuid) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, data.totalAttacks);
            stmt.setInt(3, data.totalHits);
            stmt.setDouble(4, data.totalDamage);
            stmt.setInt(5, data.heavyHits);
            stmt.setInt(6, data.slashHits);
            stmt.setInt(7, data.thrustHits);
            stmt.executeUpdate();
        }
    }

    public static class CombatStatsData {
        private int totalAttacks;
        private int totalHits;
        private double totalDamage;
        private int heavyHits;
        private int slashHits;
        private int thrustHits;

        public CombatStatsData() {
            this(0, 0, 0.0, 0, 0, 0);
        }

        public CombatStatsData(int totalAttacks, int totalHits, double totalDamage, int heavyHits, int slashHits, int thrustHits) {
            this.totalAttacks = totalAttacks;
            this.totalHits = totalHits;
            this.totalDamage = totalDamage;
            this.heavyHits = heavyHits;
            this.slashHits = slashHits;
            this.thrustHits = thrustHits;
        }

        public void incrementAttack() {
            totalAttacks++;
        }

        public void registerHit(CombatWeaponType type, double damage) {
            totalHits++;
            totalDamage += Math.max(0, damage);
            switch (type) {
                case HEAVY -> heavyHits++;
                case SLASH -> slashHits++;
                case THRUST -> thrustHits++;
            }
        }
    }
}
