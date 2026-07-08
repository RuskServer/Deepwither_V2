package com.ruskserver.deepwither_V2.core.stat;

/**
 * プレイヤーがポイントを割り振ることができる属性(Attribute)の種類。
 */
public enum AttributeType {
    STR("筋力"),
    VIT("体力"),
    MND("精神力"),
    INT("知力"),
    AGI("敏捷性");

    private final String displayName;

    /**
     * 相殺関係にある対抗属性。ある属性を上げると、この属性の有効上限が下がる。
     * 例: VIT を上げると AGI の上限が下がる。MND は独立(対抗なし)。
     */
    private AttributeType counterpart;

    AttributeType(String displayName) {
        this.displayName = displayName;
        this.counterpart = null;
    }

    AttributeType(String displayName, AttributeType counterpart) {
        this.displayName = displayName;
        this.counterpart = counterpart;
    }

    static {
        STR.counterpart = INT;
        INT.counterpart = STR;
        VIT.counterpart = AGI;
        AGI.counterpart = VIT;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AttributeType getCounterpart() {
        return counterpart;
    }
}
