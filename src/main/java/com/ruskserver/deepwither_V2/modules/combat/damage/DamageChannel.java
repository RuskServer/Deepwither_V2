package com.ruskserver.deepwither_V2.modules.combat.damage;

/**
 * 1ヒット内に同時存在できるダメージチャンネル（PoE / MythicLib 式マルチコンポーネント）。
 * <p>
 * 変換チェーンの順序は {@code PHYSICAL -> LIGHTNING -> ICE -> FIRE -> ABYSS} の一方向のみ許可する。
 * {@link #ABYSS}（深淵）および {@link #TRUE}（確定）はマジックシールド（ES）を貫通して HP へ直接通る。
 */
public enum DamageChannel {
    PHYSICAL("物理", false, false),
    LIGHTNING("雷", true, false),
    ICE("氷", true, false),
    FIRE("火", true, false),
    HOLY("聖", true, false),
    ABYSS("深淵", false, true),
    TRUE("確定", false, true);

    private final String displayName;
    private final boolean elemental;
    private final boolean bypassesMagicShield;

    DamageChannel(String displayName, boolean elemental, boolean bypassesMagicShield) {
        this.displayName = displayName;
        this.elemental = elemental;
        this.bypassesMagicShield = bypassesMagicShield;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isElemental() {
        return elemental;
    }

    /** 深淵・確定ダメージはマジックシールドを貫通して HP を直接削る */
    public boolean bypassesMagicShield() {
        return bypassesMagicShield;
    }
}
