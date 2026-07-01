package com.ruskserver.deepwither_V2.modules.dungeon.schematic;

import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDefinition;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.List;

/**
 * スケマティックから読み込み・解析済みのルームデータ。
 * <p>
 * マーカーブロックが検出・除去された後のクリップボードと、
 * 抽出されたドア・スポーンポイント・報酬位置を保持します。
 *
 * @param schematicId   スケマティック識別子（"ダンジョンID:schematicName"）
 * @param clipboard     マーカー除去済みのクリップボード
 * @param entryDoors    入口ドア（鉄ブロックから抽出）
 * @param exitDoors     出口ドア（金ブロックから抽出）
 * @param mobSpawns     モブスポーンポイント（エメラルドブロックから抽出）
 * @param lootPositions 報酬配置位置（ダイヤブロックから抽出）
 * @param entryTeleport エントリー TELEPORT 位置（ラピスブロックから抽出）
 * @param bossSpawn     ボススポーン位置（レッドストーンブロックから抽出）
 * @param origin        スケマティックの原点座標（クリップボード内の基準点）
 */
public record RoomSchematic(
        String schematicId,
        BlockArrayClipboard clipboard,
        List<DoorDefinition> entryDoors,
        List<DoorDefinition> exitDoors,
        List<BlockVector3> mobSpawns,
        List<BlockVector3> lootPositions,
        BlockVector3 entryTeleport,
        BlockVector3 bossSpawn,
        BlockVector3 origin
) {

    /**
     * 全ドアを1つのリストにまとめたものを返します。
     */
    public List<DoorDefinition> allDoors() {
        var list = new java.util.ArrayList<>(entryDoors);
        list.addAll(exitDoors);
        return list;
    }

    /**
     * このルームの接続可能ドア数を返します。
     */
    public int doorCount() {
        return entryDoors.size() + exitDoors.size();
    }

    /**
     * クリップボードの最小点・最大点から計算したサイズ（幅, 高さ, 奥行）を返します。
     */
    public BlockVector3 size() {
        var min = clipboard.getMinimumPoint();
        var max = clipboard.getMaximumPoint();
        return BlockVector3.at(
                max.x() - min.x() + 1,
                max.y() - min.y() + 1,
                max.z() - min.z() + 1
        );
    }
}
