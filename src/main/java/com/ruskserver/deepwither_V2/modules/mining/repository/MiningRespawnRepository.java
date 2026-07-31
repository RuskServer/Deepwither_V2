package com.ruskserver.deepwither_V2.modules.mining.repository;

import com.ruskserver.deepwither_V2.core.database.DatabaseManager;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Repository;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

@Repository
public class MiningRespawnRepository implements Startable {

    private final DatabaseManager databaseManager;
    private final JavaPlugin plugin;

    @Inject
    public MiningRespawnRepository(DatabaseManager databaseManager, JavaPlugin plugin) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
    }

    @Override
    public void start() {
        try (Connection connection = databaseManager.getConnection()) {
            ensureTable(connection);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize mining respawn table", exception);
        }
    }

    public List<DepletedOre> findAll() {
        List<DepletedOre> result = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection()) {
            ensureTable(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT world_id, x, y, z, material, respawn_at FROM depleted_ores")) {
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        try {
                            result.add(new DepletedOre(
                                    UUID.fromString(rows.getString("world_id")),
                                    rows.getInt("x"),
                                    rows.getInt("y"),
                                    rows.getInt("z"),
                                    Material.valueOf(rows.getString("material")),
                                    rows.getLong("respawn_at")
                            ));
                        } catch (IllegalArgumentException invalidRow) {
                            plugin.getLogger().warning("Skipped invalid depleted ore row: " + invalidRow.getMessage());
                        }
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load depleted ores", exception);
        }
        return result;
    }

    public void save(DepletedOre ore) {
        try (Connection connection = databaseManager.getConnection()) {
            ensureTable(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "MERGE INTO depleted_ores (world_id, x, y, z, material, respawn_at) "
                            + "KEY(world_id, x, y, z) VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, ore.worldId().toString());
                statement.setInt(2, ore.x());
                statement.setInt(3, ore.y());
                statement.setInt(4, ore.z());
                statement.setString(5, ore.material().name());
                statement.setLong(6, ore.respawnAtMillis());
                statement.executeUpdate();
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save depleted ore", exception);
        }
    }

    public void delete(DepletedOre ore) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM depleted_ores WHERE world_id = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, ore.worldId().toString());
            statement.setInt(2, ore.x());
            statement.setInt(3, ore.y());
            statement.setInt(4, ore.z());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete depleted ore", exception);
        }
    }

    private void ensureTable(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS depleted_ores ("
                        + "world_id VARCHAR(36) NOT NULL, "
                        + "x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, "
                        + "material VARCHAR(64) NOT NULL, "
                        + "respawn_at BIGINT NOT NULL, "
                        + "PRIMARY KEY(world_id, x, y, z))")) {
            statement.execute();
        }
    }
}
