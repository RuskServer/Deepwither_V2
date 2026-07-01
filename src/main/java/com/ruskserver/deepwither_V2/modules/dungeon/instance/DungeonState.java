package com.ruskserver.deepwither_V2.modules.dungeon.instance;

/**
 * ダンジョンの進行状態を表すenum。
 */
public enum DungeonState {
    /** 生成中 */
    GENERATING,
    /** 進行中（プレイヤーが探索中） */
    ACTIVE,
    /** クリア（ボス撃破） */
    CLEARED,
    /** タイムアウト */
    TIMED_OUT,
    /** 全滅（ライフ切れ） */
    WIPED,
    /** キャンセル済み */
    CANCELLED
}
