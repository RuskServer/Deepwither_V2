package com.ruskserver.deepwither_V2.modules.artifact;

import com.ruskserver.deepwither_V2.core.stat.StatType;

import java.util.Map;

/**
 * アーティファクトの実データを保持するクラス。
 * ItemStackのPDCと相互変換されます。
 */
public class ArtifactData {
    private final ArtifactItemType itemType;
    private final Map<StatType, Double> subStats;

    public ArtifactData(ArtifactItemType itemType, Map<StatType, Double> subStats) {
        this.itemType = itemType;
        this.subStats = subStats;
    }

    public ArtifactItemType getItemType() {
        return itemType;
    }

    public ArtifactSetType getSetType() {
        return itemType.getSetType();
    }

    public StatType getMainStat() {
        return itemType.getMainStatType();
    }

    public double getMainStatValue() {
        return itemType.getMainStatValue();
    }

    public Map<StatType, Double> getSubStats() {
        return subStats;
    }
}
