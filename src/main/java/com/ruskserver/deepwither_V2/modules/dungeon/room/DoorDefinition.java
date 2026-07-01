package com.ruskserver.deepwither_V2.modules.dungeon.room;

import com.sk89q.worldedit.math.BlockVector3;

/**
 * スケマティックから抽出されたドアの定義。
 * <p>
 * マーカーブロック（鉄ブロック=ENTRY、金ブロック=EXIT）の位置と、
 * 隣接する空気ブロックから判定された開口方向を保持します。
 *
 * @param position   ワールド上のドアブロック座標（スケマティック内の相対座標）
 * @param direction  開口方向（隣接する空気ブロックの方向）
 * @param type       ドア種別（ENTRY または EXIT）
 * @param accepts    このドアが接続可能なルームタグのリスト
 */
public record DoorDefinition(
        BlockVector3 position,
        DoorDirection direction,
        DoorType type,
        java.util.List<String> accepts
) {

    public DoorDefinition {
        if (position == null) throw new IllegalArgumentException("position は null にできません");
        if (direction == null) throw new IllegalArgumentException("direction は null にできません");
        if (type == null) throw new IllegalArgumentException("type は null にできません");
        if (accepts == null) accepts = java.util.List.of();
    }

    /**
     * このドアが指定タグのルームと接続可能か判定します。
     */
    public boolean accepts(String tag) {
        return accepts.contains(tag);
    }

    /**
     * このドアが指定タグリストのいずれかのルームと接続可能か判定します。
     */
    public boolean acceptsAny(java.util.Collection<String> tags) {
        return tags.stream().anyMatch(this::accepts);
    }
}
