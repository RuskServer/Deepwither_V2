package com.ruskserver.deepwither_V2.modules.dungeon.definition;

import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomType;

import java.util.List;

/**
 * 永劫の残氷殿。
 * <p>
 * 直線廊下と部屋のみの一本道ダンジョン。分岐なし。
 */
public class EternalIceDungeon extends DungeonDefinition {

    public EternalIceDungeon() {
        super(
                "eternal_ice",
                "永劫の残氷殿",
                "eternal_ice",
                12,
                30,
                3,
                0.0,
                0,
                List.of(
                        new RoomSlot("room_small", RoomType.ROOM_SMALL, 60, 0, List.of("room")),
                        new RoomSlot("corridor_straight", RoomType.CORRIDOR_STRAIGHT, 40, 0, List.of("corridor"))
                ),
                "boss_room"
        );
    }
}
