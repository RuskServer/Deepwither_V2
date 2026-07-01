package com.ruskserver.deepwither_V2.modules.dungeon.definition;

import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomType;

import java.util.List;

/**
 * 分岐ダンジョン定義。
 * <p>
 * T字路・十字路を持ち、複数ルートを探索するダンジョン。
 * 中級者向け。
 */
public class BranchDungeon extends DungeonDefinition {

    public BranchDungeon() {
        super(
                "branch_dungeon",
                "Labyrinth of Shadows",
                "branch_dungeon",
                15,
                45,
                5,
                0.3,
                3,
                List.of(
                        new RoomSlot("room_small", RoomType.ROOM_SMALL, 15, 4, List.of("room")),
                        new RoomSlot("corridor_straight", RoomType.CORRIDOR_STRAIGHT, 25, 0, List.of("corridor")),
                        new RoomSlot("corridor_turn", RoomType.CORRIDOR_TURN, 15, 0, List.of("corridor")),
                        new RoomSlot("corridor_t", RoomType.CORRIDOR_T, 20, 0, List.of("corridor", "branch")),
                        new RoomSlot("corridor_cross", RoomType.CORRIDOR_CROSS, 10, 0, List.of("corridor", "branch")),
                        new RoomSlot("room_medium", RoomType.ROOM_MEDIUM, 10, 3, List.of("room")),
                        new RoomSlot("treasure_room", RoomType.TREASURE, 5, 3, List.of("treasure")),
                        new RoomSlot("dead_end", RoomType.DEAD_END, 5, 0, List.of("dead_end"))
                ),
                "boss_room"
        );
    }
}
