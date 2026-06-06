package com.ruskserver.deepwither_V2.modules.character.provider;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class CharacterLocationProvider implements CharacterDataProvider<CharacterLocationProvider.CharacterLocationData> {

    public static final DataKey<CharacterLocationData> KEY = new DataKey<>("character_location_data");

    @Override
    public DataKey<CharacterLocationData> getKey() {
        return KEY;
    }

    @Override
    public CharacterLocationData loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_locations (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "world VARCHAR(64) NOT NULL, " +
                        "x DOUBLE NOT NULL, " +
                        "y DOUBLE NOT NULL, " +
                        "z DOUBLE NOT NULL, " +
                        "yaw FLOAT NOT NULL, " +
                        "pitch FLOAT NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT world, x, y, z, yaw, pitch FROM character_locations WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CharacterLocationData(
                            rs.getString("world"),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        }
        return null; // データがない場合はnull（初期スポーン位置を使用する）
    }

    @Override
    public void saveToDb(UUID characterId, CharacterLocationData data, Connection conn) throws Exception {
        if (data == null) return;
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_locations (character_id, world, x, y, z, yaw, pitch) KEY(character_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setString(2, data.getWorldName());
            stmt.setDouble(3, data.getX());
            stmt.setDouble(4, data.getY());
            stmt.setDouble(5, data.getZ());
            stmt.setFloat(6, data.getYaw());
            stmt.setFloat(7, data.getPitch());
            stmt.executeUpdate();
        }
    }

    public static class CharacterLocationData {
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        public CharacterLocationData(String worldName, double x, double y, double z, float yaw, float pitch) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public String getWorldName() { return worldName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }

        public Location toLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            return new Location(world, x, y, z, yaw, pitch);
        }

        public static CharacterLocationData fromLocation(Location loc) {
            if (loc == null || loc.getWorld() == null) return null;
            return new CharacterLocationData(
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ(),
                    loc.getYaw(),
                    loc.getPitch()
            );
        }
    }
}
