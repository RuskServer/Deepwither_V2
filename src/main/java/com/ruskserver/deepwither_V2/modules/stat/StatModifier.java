package com.ruskserver.deepwither_V2.modules.stat;

/**
 * 1つのステータス変動要因（バフ、デバフ、装備ステータスなど）を表す不変クラス。
 */
public class StatModifier {
    private final String sourceId;
    private final double value;
    private final ModifierType type;

    public StatModifier(String sourceId, double value, ModifierType type) {
        this.sourceId = sourceId;
        this.value = value;
        this.type = type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public double getValue() {
        return value;
    }

    public ModifierType getType() {
        return type;
    }
}
