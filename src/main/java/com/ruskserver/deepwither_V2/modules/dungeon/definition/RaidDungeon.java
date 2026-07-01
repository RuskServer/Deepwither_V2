package com.ruskserver.deepwither_V2.modules.dungeon.definition;

import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomType;

import java.util.List;

/**
 * レイドダンジョン定義。
 * <p>
 * 大規模で長時間のダンジョン。分岐率が高く、高難易度。
 * 上級者パーティ向け。
 */
public class RaidDungeon extends DungeonDefinition {

    public RaidDungeon() {
        super(
                "raid_dungeon",
                "Abyssal Fortress",
                "raid_dungeon",
                20,
                60,
                8,
                0.4,
                4,
                List.of(
                        new RoomSlot("room_large", RoomType.ROOM_MEDIUM, 15, 5, List.of("room")),
                        new RoomSlot("corridor_straight", RoomType.CORRIDOR_STRAIGHT, 20, 0, List.of("corridor")),
                        new RoomSlot("corridor_turn", RoomType.CORRIDOR_TURN, 15, 0, List.of("corridor")),
                        new RoomSlot("corridor_t", RoomType.CORRIDOR_T, 25, 0, List.of("corridor", "branch")),
                        new RoomSlot("corridor_cross", RoomType.CORRIDOR_CROSS, 15, 0, List.of("corridor", "branch")),
                        new RoomSlot("room_arena", RoomType.ROOM_MEDIUM, 10, 2, List.of("room", "arena")),
                        new RoomSlot("treasure_room", RoomType.TREASURE, 8, 4, List.of("treasure")),
                        new RoomSlot("trap_room", RoomType.ROOM_SMALL, 10, 3, List.of("room", "trap")),
                        new RoomSlot("dead_end", RoomType.DEAD_END, 5, 0, List.of("dead_end"))
                ),
                "boss_room"
        );
    }
}
