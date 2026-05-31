package com.ruskserver.deepwither_V2.modules.lootchest.api;

import java.util.List;

/**
 * ルートテーブルの定義インターフェース。
 * Java側でハードコード定義する際に使用します。
 */
public interface LootTableDefinition {
    /**
     * ルートテーブルの一意識別子。
     */
    String getId();

    /**
     * 表示名。
     */
    String getDisplayName();

    /**
     * 抽選されるアイテムのリスト。
     */
    List<LootItem> getLootItems();

    /**
     * 一度に生成されるアイテムの数（ロール回数）。
     */
    int getRolls();
}
