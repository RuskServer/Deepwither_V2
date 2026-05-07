package com.ruskserver.deepwither_V2.modules.trader.api;

import java.util.List;

/**
 * トレーダーの定義インターフェース。
 * Citizens NPC の名前に紐づけて、販売商品を管理します。
 */
public interface TraderDefinition {

    /**
     * このトレーダーに対応する Citizens NPC の名前。
     * 例: "Lunaris Atelier"
     * @return NPC名
     */
    String getNpcName();

    /**
     * このトレーダーが販売する商品リスト。
     * @return TraderProduct のリスト
     */
    List<TraderProduct> getProducts();

    /**
     * このトレーダーの表示名（UI に表示）。
     * @return 表示名
     */
    String getDisplayName();
}

