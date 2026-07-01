package com.ruskserver.deepwither_V2.modules.dungeon.definition;

import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomType;

import java.util.List;

/**
 * ダンジョンテンプレート内の1ルームスロットを定義します。
 * <p>
 * 各スロットは、使用するスケマティックファイル名、ルーム種別、出現重み、最大配置数、タグを保持します。
 * ダンジョン定義クラスでこのレコードをリストとして構築し、使用可能なルームを定義します。
 *
 * @param schematicName スケマティックファイル名（拡張子なし。例: "corridor_straight"）
 * @param type          ルーム種別
 * @param weight        出現重み（0 の場合、自動選択されません。手動配置や特殊ルーム向け）
 * @param maxCount      最大配置数（0 の場合、無制限）
 * @param tags          ルームタグ（例: "corridor", "room", "treasure"）
 */
public record RoomSlot(
        String schematicName,
        RoomType type,
        int weight,
        int maxCount,
        List<String> tags
) {

    public RoomSlot {
        if (schematicName == null || schematicName.isBlank()) {
            throw new IllegalArgumentException("schematicName は空にできません");
        }
        if (type == null) {
            throw new IllegalArgumentException("type は null にできません");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weight は0以上でなければなりません: " + schematicName);
        }
        if (maxCount < 0) {
            throw new IllegalArgumentException("maxCount は0以上でなければなりません: " + schematicName);
        }
        if (tags == null) {
            tags = List.of();
        }
    }

    /**
     * このスロットが自動選択可能か（重みが正かつルーム種別が分岐ルームでない、または分岐が有効な場合）を判定します。
     */
    public boolean isAutoSelectable(boolean branchingEnabled) {
        if (weight <= 0) return false;
        if (!branchingEnabled && (type == RoomType.CORRIDOR_T || type == RoomType.CORRIDOR_CROSS)) {
            return false;
        }
        return true;
    }
}
