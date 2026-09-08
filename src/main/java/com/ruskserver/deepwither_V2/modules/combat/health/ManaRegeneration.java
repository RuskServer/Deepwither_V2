package com.ruskserver.deepwither_V2.modules.combat.health;

public final class ManaRegeneration {
    public static final double COMBAT_RATE = 0.02;
    public static final double OUT_OF_COMBAT_BONUS = 0.03;

    private ManaRegeneration() {}

    public static double rate(boolean inCombat) {
        return COMBAT_RATE + (inCombat ? 0 : OUT_OF_COMBAT_BONUS);
    }

    public static double regenerate(double current, double maximum, boolean inCombat) {
        return Math.min(maximum, current + maximum * rate(inCombat));
    }
}
