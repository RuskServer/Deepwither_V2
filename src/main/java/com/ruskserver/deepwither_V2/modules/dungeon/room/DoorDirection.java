package com.ruskserver.deepwither_V2.modules.dungeon.room;

import com.sk89q.worldedit.math.BlockVector3;

/**
 * ドアの開口方向を表すenum。
 * マーカーブロック（鉄ブロック/金ブロック）から隣接する空気ブロックの方向で自動判定されます。
 */
public enum DoorDirection {
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0);

    private final BlockVector3 offset;

    DoorDirection(int x, int y, int z) {
        this.offset = BlockVector3.at(x, y, z);
    }

    public BlockVector3 getOffset() {
        return offset;
    }

    /**
     * この方向と反対方向を返します。
     */
    public DoorDirection opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    /**
     * この方向を指定角度（90度単位、時計回り）で回転させた方向を返します。
     *
     * @param rotation 回転角度（90, 180, 270）
     */
    public DoorDirection rotate(int rotation) {
        int normalized = ((rotation % 360) + 360) % 360;
        return switch (normalized) {
            case 90 -> switch (this) {
                case NORTH -> EAST;
                case EAST -> SOUTH;
                case SOUTH -> WEST;
                case WEST -> NORTH;
            };
            case 180 -> switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
            case 270 -> switch (this) {
                case NORTH -> WEST;
                case WEST -> SOUTH;
                case SOUTH -> EAST;
                case EAST -> NORTH;
            };
            default -> this;
        };
    }
}
