package com.ruskserver.deepwither_V2.core.database.character;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * キャラクターの全データを集約する型安全なコンテナ。
 * DirtyFlagの概念を持ち、変更があったデータのみをDBに保存できるようにします。
 */
public class CharacterData {
    private final UUID characterId;
    private final Map<DataKey<?>, Object> dataMap = new ConcurrentHashMap<>();
    private final Set<DataKey<?>> dirtyKeys = ConcurrentHashMap.newKeySet();

    public CharacterData(UUID characterId) {
        this.characterId = characterId;
    }

    public UUID getCharacterId() {
        return characterId;
    }

    /**
     * 指定されたキーのデータを取得します。
     */
    @SuppressWarnings("unchecked")
    public <T> T get(DataKey<T> key) {
        return (T) dataMap.get(key);
    }

    /**
     * 指定されたキーにデータを設定し、そのキーをDirty（変更あり）としてマークします。
     */
    public <T> void set(DataKey<T> key, T value) {
        dataMap.put(key, value);
        markDirty(key);
    }
    
    /**
     * データベースから読み込んだ初期データを設定します（Dirtyフラグは立てません）。
     */
    public <T> void setInitial(DataKey<T> key, T value) {
        dataMap.put(key, value);
    }

    /**
     * データの中身を直接書き換えた場合など、明示的にDirtyフラグを立てたい場合に使用します。
     */
    public void markDirty(DataKey<?> key) {
        dirtyKeys.add(key);
    }

    /**
     * 現在Dirtyとしてマークされているキーのセットを返します。
     */
    public Set<DataKey<?>> getDirtyKeys() {
        return Collections.unmodifiableSet(dirtyKeys);
    }

    /**
     * すべてのDirtyフラグをクリアします（DB保存後に呼び出されます）。
     */
    public void clearDirtyFlags() {
        dirtyKeys.clear();
    }
    
    /**
     * 特定のキーのDirtyフラグをクリアします。
     */
    public void clearDirtyFlag(DataKey<?> key) {
        dirtyKeys.remove(key);
    }
}
