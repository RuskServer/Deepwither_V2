package com.ruskserver.deepwither_V2.modules.dungeon.definition;

import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomType;

import java.util.List;

/**
 * シンプルダンジョン定義。
 * <p>
 * 一本道（分岐なし）の基本的なダンジョン。
 * 初心者向け・テスト用。
 */
public class SimpleDungeon extends DungeonDefinition {

    public SimpleDungeon() {
        super(
                "simple_dungeon",
                "Simple Dungeon",
                "simple_dungeon",
                10,
                30,
                3,
                0.0,
                0,
                List.of(
                        new RoomSlot("room_small", RoomType.ROOM_SMALL, 15, 3, List.of("room")),
                        new RoomSlot("corridor_straight", RoomType.CORRIDOR_STRAIGHT, 30, 0, List.of("corridor")),
                        new RoomSlot("corridor_turn", RoomType.CORRIDOR_TURN, 20, 0, List.of("corridor")),
                        new RoomSlot("room_medium", RoomType.ROOM_MEDIUM, 10, 2, List.of("room")),
                        new RoomSlot("treasure_room", RoomType.TREASURE, 5, 2, List.of("treasure")),
                        new RoomSlot("dead_end", RoomType.DEAD_END, 10, 0, List.of("dead_end"))
                ),
                "boss_room"
        );
    }
}
