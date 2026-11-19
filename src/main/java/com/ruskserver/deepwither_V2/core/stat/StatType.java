package com.ruskserver.deepwither_V2.core.stat;

/**
 * 武器や防具、プレイヤーなどのステータス種類を定義する列挙型。
 */
public enum StatType {
    ATTACK_DAMAGE("攻撃力"),
    RANGED_DAMAGE("射撃ダメージ"),
    DEFENSE("防御力"),
    MAGIC_DAMAGE("魔法攻撃力"),
    MAGIC_DEFENSE("魔法防御力"),
    CRITICAL_CHANCE("クリティカル率"),
    CRITICAL_DAMAGE("クリティカルダメージ"),
    HEALTH("最大HP"),
    MAX_MANA("最大マナ"),
    ATTACK_SPEED("攻撃速度"),
    SPEED("移動速度"),
    COOLDOWN_REDUCTION("クールタイム短縮"),
    FIRE_DAMAGE("火属性攻撃力"),
    ICE_DAMAGE("氷属性攻撃力"),
    LIGHTNING_DAMAGE("雷属性攻撃力"),
    HOLY_DAMAGE("聖属性攻撃力"),
    ABYSS_DAMAGE("深淵属性攻撃力"),
    PHYSICAL_DAMAGE_REDUCTION("物理ダメージ軽減"),

    // PoE 式防御・マジックシールド (ES)
    MAX_MAGIC_SHIELD("最大マジックシールド"),
    MAGIC_SHIELD_RECHARGE_RATE("マジックシールド回復速度"),
    EVASION("回避率"),
    BLOCK_CHANCE("ブロック率"),

    // 属性耐性 (基本上限 75%, MAX_ELEMENTAL_RESISTANCE で最大 90% まで拡張)
    FIRE_RESISTANCE("火属性耐性"),
    ICE_RESISTANCE("氷属性耐性"),
    LIGHTNING_RESISTANCE("雷属性耐性"),
    HOLY_RESISTANCE("聖属性耐性"),
    ABYSS_RESISTANCE("深淵耐性"),
    MAX_ELEMENTAL_RESISTANCE("属性耐性上限"),

    // 防御・耐性貫通
    ARMOR_PENETRATION("物理防御貫通"),
    ELEMENTAL_PENETRATION("属性耐性貫通"),
    ABYSS_PENETRATION("深淵耐性貫通"),

    // 属性変換 (0..100 %, 物理 -> 雷 -> 氷 -> 火 -> 深淵 の一方向チェーン)
    PHYS_TO_LIGHTNING_CONVERSION("物理→雷変換"),
    PHYS_TO_ICE_CONVERSION("物理→氷変換"),
    PHYS_TO_FIRE_CONVERSION("物理→火変換"),
    PHYS_TO_ABYSS_CONVERSION("物理→深淵変換"),
    ELEMENTAL_TO_ABYSS_CONVERSION("属性→深淵変換"),

    // 汎用 Increased / More ダメージ倍率 (例: 20.0 = +20%)
    INCREASED_DAMAGE("ダメージ増加"),
    INCREASED_ELEMENTAL_DAMAGE("属性ダメージ増加");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
