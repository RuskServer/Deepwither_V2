package com.ruskserver.deepwither_V2.modules.mob.region;

/**
 * スポーンテーブルの1エントリ。モブIDとその出現重みを保持します。
 *
 * @param mobId  モブID（{@link com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager} に登録済みのID）
 * @param weight 出現重み（正の整数。大きいほど選ばれやすい）
 */
public record SpawnEntry(String mobId, int weight) {

    public SpawnEntry {
        if (mobId == null || mobId.isBlank()) throw new IllegalArgumentException("mobId は空にできません");
        if (weight <= 0) throw new IllegalArgumentException("weight は1以上でなければなりません: " + mobId);
    }
}
