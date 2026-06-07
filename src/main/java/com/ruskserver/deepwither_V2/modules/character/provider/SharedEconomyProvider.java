package com.ruskserver.deepwither_V2.modules.character.provider;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * 標準/SHCキャラクターの「共有残高」をプレイヤー単位でDBに保存するプロバイダー。
 * 真HCキャラ切替時に共有残高を退避し、クラッシュ復旧時に復元するために使用します。
 */
@Component
public class SharedEconomyProvider implements PlayerDataProvider<Double> {

    public static final DataKey<Double> KEY = new DataKey<>("shared_economy_balance");

    @Override
    public DataKey<Double> getKey() {
        return KEY;
    }

    @Override
    public Double loadFromDb(UUID playerUuid, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS shared_economy (" +
                        "player_uuid VARCHAR(36) PRIMARY KEY, " +
                        "balance DOUBLE NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT balance FROM shared_economy WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        return null;
    }

    @Override
    public void saveToDb(UUID playerUuid, Double balance, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO shared_economy (player_uuid, balance) KEY(player_uuid) VALUES (?, ?)")) {
            stmt.setString(1, playerUuid.toString());
            stmt.setDouble(2, balance != null ? balance : 0.0);
            stmt.executeUpdate();
        }
    }
}
