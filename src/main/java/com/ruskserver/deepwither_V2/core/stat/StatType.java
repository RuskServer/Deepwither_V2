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
    PHYSICAL_DAMAGE_REDUCTION("物理ダメージ軽減");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
