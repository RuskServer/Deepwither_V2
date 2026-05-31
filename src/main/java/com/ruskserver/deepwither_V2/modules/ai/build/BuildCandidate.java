package com.ruskserver.deepwither_V2.modules.ai.build;

import com.ruskserver.deepwither_V2.core.stat.StatType;

import java.util.EnumMap;
import java.util.Map;

public class BuildCandidate implements Comparable<BuildCandidate> {

    private final Map<String, String> slots = new java.util.LinkedHashMap<>();
    private final Map<StatType, Double> finalStats = new EnumMap<>(StatType.class);
    private double score;

    public void setSlot(String slotName, String itemId) {
        slots.put(slotName, itemId);
    }

    public String getSlot(String slotName) {
        return slots.get(slotName);
    }

    public Map<String, String> getSlots() {
        return slots;
    }

    public void setFinalStat(StatType type, double value) {
        finalStats.put(type, value);
    }

    public double getFinalStat(StatType type) {
        return finalStats.getOrDefault(type, 0.0);
    }

    public Map<StatType, Double> getFinalStats() {
        return finalStats;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public double getPhysicalReduction() {
        double def = getFinalStat(StatType.DEFENSE);
        return 250.0 / (250.0 + def);
    }

    public double getMagicReduction() {
        double mdef = getFinalStat(StatType.MAGIC_DEFENSE);
        return 250.0 / (250.0 + mdef);
    }

    @Override
    public int compareTo(BuildCandidate o) {
        return Double.compare(o.score, this.score);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("装備構成:\n");
        slots.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        sb.append("最終ステータス:\n");
        finalStats.forEach((k, v) -> sb.append("  ").append(k.getDisplayName()).append(": ").append(String.format("%.1f", v)).append("\n"));
        sb.append("物理軽減率: ").append(String.format("%.1f", (1 - getPhysicalReduction()) * 100)).append("%\n");
        sb.append("魔法軽減率: ").append(String.format("%.1f", (1 - getMagicReduction()) * 100)).append("%\n");
        sb.append("スコア: ").append(String.format("%.1f", score)).append("\n");
        return sb.toString();
    }
}
