package com.ruskserver.deepwither_V2.modules.lootchest.repository;

import com.ruskserver.deepwither_V2.core.database.DatabaseManager;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Repository;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.lootchest.api.PlayerChestData;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository
public class PlayerChestRepository implements Startable {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    @Inject
    public PlayerChestRepository(DatabaseManager databaseManager, org.bukkit.plugin.java.JavaPlugin plugin) {
        this.databaseManager = databaseManager;
        this.logger = plugin.getLogger();
    }

    @Override
    public void start() {
        createTable();
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS player_chest_cooldowns (
                    player_uuid UUID NOT NULL,
                    chest_id UUID NOT NULL,
                    next_open_time TIMESTAMP NOT NULL,
                    PRIMARY KEY (player_uuid, chest_id)
                );
                """;
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create player_chest_cooldowns table!", e);
        }
    }

    public void save(PlayerChestData data) {
        String sql = """
                MERGE INTO player_chest_cooldowns (player_uuid, chest_id, next_open_time)
                KEY(player_uuid, chest_id)
                VALUES (?, ?, ?);
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, data.getPlayerUuid());
            pstmt.setObject(2, data.getChestId());
            pstmt.setTimestamp(3, Timestamp.valueOf(data.getNextOpenTime()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save player chest data!", e);
        }
    }

    public Optional<PlayerChestData> findByPlayerAndChest(UUID playerUuid, UUID chestId) {
        String sql = "SELECT * FROM player_chest_cooldowns WHERE player_uuid = ? AND chest_id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, playerUuid);
            pstmt.setObject(2, chestId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("next_open_time");
                return Optional.of(new PlayerChestData(playerUuid, chestId, ts.toLocalDateTime()));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to find player chest data!", e);
        }
        return Optional.empty();
    }
}
