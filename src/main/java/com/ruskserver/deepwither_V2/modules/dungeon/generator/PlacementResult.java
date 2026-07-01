package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.sk89q.worldedit.math.BlockVector3;

import java.util.List;

/**
 * ルーム配置結果。
 * 配置された入口ドア、モブスポーン位置、報酬位置のワールド座標を保持します。
 */
public record PlacementResult(
        BlockVector3 entryDoorWorldPos,
        List<BlockVector3> mobSpawnWorldPositions,
        List<BlockVector3> lootWorldPositions,
        BlockVector3 bossSpawnWorldPos
) {}
