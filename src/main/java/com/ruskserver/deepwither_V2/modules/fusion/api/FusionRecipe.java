package com.ruskserver.deepwither_V2.modules.fusion.api;

import java.util.Map;

/**
 * 合成レシピの基底となるインターフェース。
 * 各合成レシピはこのインターフェースを実装します。
 */
public interface FusionRecipe {

    /**
     * @return レシピの一意のID（例: "powerful_sword_fusion"）
     */
    String getId();

    /**
     * @return 合成に必要な素材アイテムのIDと必要数のマップ。
     */
    Map<String, Integer> getIngredients();

    /**
     * @return 合成結果アイテムのID。
     */
    String getResultItemId();

    /**
     * @return 合成結果アイテムの数。
     */
    default int getResultAmount() {
        return 1;
    }

    /**
     * @return このレシピを提供する合成屋NPCのID。
     */
    String getFusionNpcId();

    /**
     * @return 合成に必要なプレイヤーレベル。0以下の場合はレベル制限なし。
     */
    default int getRequiredLevel() {
        return 0;
    }

    /**
     * @return 合成に必要なスキルIDとレベルのマップ。空の場合はスキル制限なし。
     */
    default Map<String, Integer> getRequiredSkills() {
        return Map.of();
    }
}
