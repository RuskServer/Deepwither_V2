package com.ruskserver.deepwither_V2.modules.stat;

/**
 * ステータスモディファイアの計算方法を定義する列挙型。
 */
public enum ModifierType {
    /**
     * 加算（例: 剣を装備して攻撃力 +20）
     * 最終ステータスは (Base + AdditiveSum) * (1.0 + MultiplicativeSum) となります。
     */
    ADDITIVE,

    /**
     * 乗算（例: バフで攻撃力 +10% -> 値としては 0.1 を指定）
     */
    MULTIPLICATIVE
}
