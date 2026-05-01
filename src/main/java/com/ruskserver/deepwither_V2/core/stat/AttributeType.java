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

    AttributeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
