package com.ruskserver.deepwither_V2.modules.mob.region;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class SafeZoneSpawnProvider implements PlayerDataProvider<Location> {

    public static final DataKey<Location> KEY = new DataKey<>("safe_zone_spawn");

    @Override
    public DataKey<Location> getKey() {
        return KEY;
    }

    @Override
    public Location loadFromDb(UUID uuid, Connection conn) throws Exception {
        conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS safe_zone_spawns (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "world VARCHAR(64), " +
                        "x DOUBLE, y DOUBLE, z DOUBLE, " +
                        "yaw FLOAT, pitch FLOAT)"
        );

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT world, x, y, z, yaw, pitch FROM safe_zone_spawns WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    World world = Bukkit.getWorld(rs.getString("world"));
                    if (world != null) {
                        return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                                rs.getFloat("yaw"), rs.getFloat("pitch"));
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void saveToDb(UUID uuid, Location location, Connection conn) throws Exception {
        if (location == null || location.getWorld() == null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM safe_zone_spawns WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "MERGE INTO safe_zone_spawns (uuid, world, x, y, z, yaw, pitch) " +
                        "KEY(uuid) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, location.getWorld().getName());
            ps.setDouble(3, location.getX());
            ps.setDouble(4, location.getY());
            ps.setDouble(5, location.getZ());
            ps.setFloat(6, location.getYaw());
            ps.setFloat(7, location.getPitch());
            ps.executeUpdate();
        }
    }
}
