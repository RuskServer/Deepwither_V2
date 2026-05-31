package com.ruskserver.deepwither_V2.modules.ai.build;

import com.ruskserver.deepwither_V2.core.stat.StatType;

import java.util.ArrayList;
import java.util.List;

public class BuildGoal {

    private final StatType primaryStat;
    private final StatType secondaryStat;
    private final double primaryWeight;
    private final int vit;
    private final List<String> passives;

    public BuildGoal(StatType primaryStat, StatType secondaryStat, double primaryWeight, int vit, List<String> passives) {
        this.primaryStat = primaryStat;
        this.secondaryStat = secondaryStat;
        this.primaryWeight = primaryWeight;
        this.vit = vit;
        this.passives = passives;
    }

    public StatType getPrimaryStat() { return primaryStat; }
    public StatType getSecondaryStat() { return secondaryStat; }
    public double getPrimaryWeight() { return primaryWeight; }
    public int getVit() { return vit; }
    public List<String> getPassives() { return passives; }

    public static BuildGoal prioritize(StatType primary) {
        return new BuildGoal(primary, null, 1.0, 0, List.of());
    }

    public static BuildGoal balanced(StatType primary, StatType secondary, double primaryWeight) {
        return new BuildGoal(primary, secondary, primaryWeight, 0, List.of());
    }
}
