package com.ruskserver.deepwither_V2.modules.combat.stagger;

public final class StaggerState {

    public enum TickResult {
        NONE,
        RECOVERED
    }

    private double current;
    private int ticksSinceGain;
    private int staggerTicksRemaining;
    private int immunityTicksRemaining;

    public boolean add(double amount, StaggerProfile profile) {
        if (amount <= 0.0 || isStaggered() || isImmune()) {
            return false;
        }

        ticksSinceGain = 0;
        current = Math.min(profile.maxStagger(), current + amount);
        if (current < profile.maxStagger()) {
            return false;
        }

        current = profile.maxStagger();
        staggerTicksRemaining = profile.staggerDurationTicks();
        return true;
    }

    public TickResult tick(StaggerProfile profile) {
        if (staggerTicksRemaining > 0) {
            staggerTicksRemaining--;
            if (staggerTicksRemaining == 0) {
                current = 0.0;
                immunityTicksRemaining = profile.recoveryImmunityTicks();
                ticksSinceGain = 0;
                return TickResult.RECOVERED;
            }
            return TickResult.NONE;
        }

        if (immunityTicksRemaining > 0) {
            immunityTicksRemaining--;
            return TickResult.NONE;
        }

        if (current > 0.0) {
            ticksSinceGain++;
            if (ticksSinceGain > profile.decayDelayTicks()) {
                current = Math.max(0.0, current - profile.decayPerTick());
            }
        }
        return TickResult.NONE;
    }

    public void resetWithImmunity(int immunityTicks) {
        current = 0.0;
        ticksSinceGain = 0;
        staggerTicksRemaining = 0;
        immunityTicksRemaining = Math.max(0, immunityTicks);
    }

    public double getCurrent() {
        return current;
    }

    public int getStaggerTicksRemaining() {
        return staggerTicksRemaining;
    }

    public int getImmunityTicksRemaining() {
        return immunityTicksRemaining;
    }

    public boolean isStaggered() {
        return staggerTicksRemaining > 0;
    }

    public boolean isImmune() {
        return immunityTicksRemaining > 0;
    }
}
