package com.ruskserver.deepwither_V2.modules.dungeon.room;

/**
 * ルームテンプレートの種別を表すenum。
 * <p>
 * branchChance が 0.0 の場合、CORRIDOR_T と CORRIDOR_CROSS は候補に入りません。
 */
public enum RoomType {
    CORRIDOR_STRAIGHT,
    CORRIDOR_TURN,
    CORRIDOR_T,
    CORRIDOR_CROSS,
    ROOM_SMALL,
    ROOM_MEDIUM,
    DEAD_END,
    TREASURE,
    BOSS
}
