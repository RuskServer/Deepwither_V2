package com.ruskserver.deepwither_V2.modules.combat.stagger;

import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;

public record StaggerProfile(
        double maxStagger,
        int staggerDurationTicks,
        int recoveryImmunityTicks,
        int decayDelayTicks,
        double decayPerTick,
        double staggeredDamageMultiplier,
        double maxStaggerPerHit,
        double physicalMultiplier,
        double rangedMultiplier,
        double magicMultiplier,
        double trueDamageMultiplier
) {

    public StaggerProfile {
        if (maxStagger <= 0.0 || staggerDurationTicks <= 0 || recoveryImmunityTicks < 0
                || decayDelayTicks < 0 || decayPerTick < 0.0 || staggeredDamageMultiplier < 1.0
                || maxStaggerPerHit <= 0.0) {
            throw new IllegalArgumentException("Invalid stagger profile");
        }
    }

    public double multiplierFor(DamageType type) {
        return switch (type) {
            case PHYSICAL -> physicalMultiplier;
            case RANGED -> rangedMultiplier;
            case MAGIC -> magicMultiplier;
            case TRUE_DAMAGE -> trueDamageMultiplier;
            case ENVIRONMENTAL -> 0.0;
        };
    }
}
