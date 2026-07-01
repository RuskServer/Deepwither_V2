package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.schematic.RoomSchematic;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * ドアの接続状態を追跡する値クラス。
 * <p>
 * 未接続ドアから次に配置するルームを選択し、接続を確立する際に使用します。
 *
 * @param roomSchematic    接続元のルームスケマティック
 * @param door             接続元のドア定義
 * @param worldPosition    ドアのワールド座標（配置済みルームのドア位置）
 * @param connectionTarget 接続先のドアが向いている方向（新しいルームの入口が来るべき方向）
 */
public record DoorConnection(
        RoomSchematic roomSchematic,
        DoorDefinition door,
        BlockVector3 worldPosition,
        DoorConnectionTarget connectionTarget
) {

    /**
     * このドアの接続先に配置する新しいルームの入口ドアが向いているべき方向を返します。
     * 例: 接続元ドアが南向きなら、新しいルームの入口ドアは北向きでなければなりません。
     */
    public com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDirection getExpectedEntryDirection() {
        return door.direction().opposite();
    }

    /**
     * 新しいルームを配置するワールド座標を計算します。
     *
     * @param entryDoorPosition 新しいルームの入口ドアのスケマティック内座標
     * @param rotation          回転角度（90度単位）
     * @return 配置座標
     */
    public BlockVector3 calculatePlacement(BlockVector3 entryDoorPosition, int rotation) {
        BlockVector3 rotatedOffset = rotatePosition(entryDoorPosition, rotation);
        return worldPosition.add(door.direction().getOffset()).subtract(rotatedOffset);
    }

    /**
     * 位置を原点周りに回転させます（90度単位、Y軸回転）。
     */
    private BlockVector3 rotatePosition(BlockVector3 pos, int rotation) {
        int normalized = ((rotation % 360) + 360) % 360;
        return switch (normalized) {
            case 90 -> BlockVector3.at(-pos.z(), pos.y(), pos.x());
            case 180 -> BlockVector3.at(-pos.x(), pos.y(), -pos.z());
            case 270 -> BlockVector3.at(pos.z(), pos.y(), -pos.x());
            default -> pos;
        };
    }

    /**
     * 接続先の情報を保持する内部レコード。
     * 実際の配置計算では {@link DoorConnection#getExpectedEntryDirection()} を使用します。
     */
    public record DoorConnectionTarget(
            com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDirection expectedEntryDirection
    ) {
    }
}
