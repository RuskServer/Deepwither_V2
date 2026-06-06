package com.ruskserver.deepwither_V2.core.database.character;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;

import java.sql.Connection;
import java.util.UUID;

/**
 * キャラクターデータの一部を提供するモジュール用インターフェース。
 * 各モジュールでこれを実装し、@Component を付与することで、起動時に自動収集されます。
 * @param <T> 提供するデータの型
 */
public interface CharacterDataProvider<T> {

    /**
     * このプロバイダーが担当するデータのキーを返します。
     */
    DataKey<T> getKey();

    /**
     * データベースから指定したキャラクターのデータを読み込みます。
     */
    T loadFromDb(UUID characterId, Connection conn) throws Exception;

    /**
     * データベースへ指定したキャラクターのデータを保存（または更新）します。
     * ※このメソッドは、対象の DataKey の DirtyFlag が立っている場合にのみ呼ばれます。
     */
    void saveToDb(UUID characterId, T data, Connection conn) throws Exception;
}
