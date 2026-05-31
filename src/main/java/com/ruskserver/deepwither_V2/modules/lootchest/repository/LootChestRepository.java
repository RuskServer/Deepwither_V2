package com.ruskserver.deepwither_V2.modules.lootchest.repository;

import com.ruskserver.deepwither_V2.core.database.DatabaseManager;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Repository;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootChestLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository
public class LootChestRepository implements Startable {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    @Inject
    public LootChestRepository(DatabaseManager databaseManager, org.bukkit.plugin.java.JavaPlugin plugin) {
        this.databaseManager = databaseManager;
        this.logger = plugin.getLogger();
    }

    @Override
    public void start() {
        createTable();
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS loot_chests (
                    id UUID PRIMARY KEY,
                    world_name VARCHAR(255) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    loot_table_id VARCHAR(255) NOT NULL,
                    next_spawn_time TIMESTAMP,
                    spawned BOOLEAN NOT NULL
                );
                """;
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create loot_chests table!", e);
        }
    }

    public void save(LootChestLocation lootChest) {
        String sql = """
                MERGE INTO loot_chests (id, world_name, x, y, z, loot_table_id, next_spawn_time, spawned)
                KEY(id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, lootChest.getId());
            pstmt.setString(2, lootChest.getLocation().getWorld().getName());
            pstmt.setDouble(3, lootChest.getLocation().getX());
            pstmt.setDouble(4, lootChest.getLocation().getY());
            pstmt.setDouble(5, lootChest.getLocation().getZ());
            pstmt.setString(6, lootChest.getLootTableId());
            pstmt.setTimestamp(7, lootChest.getNextSpawnTime() != null ? Timestamp.valueOf(lootChest.getNextSpawnTime()) : null);
            pstmt.setBoolean(8, lootChest.isSpawned());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save loot chest!", e);
        }
    }

    public List<LootChestLocation> findAll() {
        List<LootChestLocation> results = new ArrayList<>();
        String sql = "SELECT * FROM loot_chests;";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String worldName = rs.getString("world_name");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                String lootTableId = rs.getString("loot_table_id");
                Timestamp ts = rs.getTimestamp("next_spawn_time");
                LocalDateTime nextSpawnTime = ts != null ? ts.toLocalDateTime() : null;
                boolean spawned = rs.getBoolean("spawned");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);
                    results.add(new LootChestLocation(id, loc, lootTableId, nextSpawnTime, spawned));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load loot chests!", e);
        }
        return results;
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM loot_chests WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete loot chest!", e);
        }
    }
}
