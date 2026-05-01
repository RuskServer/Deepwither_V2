package com.ruskserver.deepwither_V2.modules.combat.damage;

/**
 * ダメージの種類を定義する列挙型。
 * それぞれのタイプに応じた防御力計算が行われます。
 */
public enum DamageType {
    /**
     * 物理ダメージ (DEFENSE で軽減)
     */
    PHYSICAL,

    /**
     * 魔法ダメージ (MAGIC_DEFENSE で軽減)
     */
    MAGIC,

    /**
     * 固定（貫通）ダメージ (防御力による軽減を完全に無視)
     */
    TRUE_DAMAGE,

    /**
     * 環境ダメージ (落下・炎・毒など。最大HPに対する割合ダメージなどとして処理される)
     */
    ENVIRONMENTAL
}
