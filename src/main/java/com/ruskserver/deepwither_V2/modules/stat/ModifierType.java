package com.ruskserver.deepwither_V2.modules.stat;

/**
 * ステータスモディファイアの計算方法を定義する列挙型。
 * <p>
 * 最終値 = AdditiveSum × (1.0 + MultiplicativeSum[Increased]) × Π(1.0 + More_i)
 */
public enum ModifierType {
    /**
     * 基礎加算（例: 剣を装備して攻撃力 +20）
     */
    ADDITIVE,

    /**
     * 加算割合増加 / PoE の Increased（例: +10% と +20% が合算されて ×1.30）
     */
    MULTIPLICATIVE,

    /**
     * 独立乗算 / PoE の More（例: +20% More と +30% More が独立乗算されて 1.20 × 1.30 = ×1.56）
     */
    MORE
}
