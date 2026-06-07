package com.ruskserver.deepwither_V2.modules.character.provider;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataProvider;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * キャラクター単位のVault経済残高をDBに保存・復元するプロバイダー。
 * 主に真ハードコア（TRUE_HARDCORE）キャラクターの資金分離に使用します。
 */
@Component
public class CharacterEconomyProvider implements CharacterDataProvider<Double> {

    public static final DataKey<Double> KEY = new DataKey<>("character_economy_balance");

    /** 新規キャラクターの初期所持金 */
    public static final double INITIAL_BALANCE = 0.0;

    @Override
    public DataKey<Double> getKey() {
        return KEY;
    }

    @Override
    public Double loadFromDb(UUID characterId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS character_economy (" +
                        "character_id VARCHAR(36) PRIMARY KEY, " +
                        "balance DOUBLE NOT NULL)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT balance FROM character_economy WHERE character_id = ?")) {
            stmt.setString(1, characterId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        return null;
    }

    @Override
    public void saveToDb(UUID characterId, Double balance, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO character_economy (character_id, balance) KEY(character_id) VALUES (?, ?)")) {
            stmt.setString(1, characterId.toString());
            stmt.setDouble(2, balance != null ? balance : INITIAL_BALANCE);
            stmt.executeUpdate();
        }
    }
}
