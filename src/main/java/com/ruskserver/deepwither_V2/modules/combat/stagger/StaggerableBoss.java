package com.ruskserver.deepwither_V2.modules.combat.stagger;

import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import org.bukkit.Color;

public interface StaggerableBoss {

    StaggerProfile getStaggerProfile();

    StaggerState getStaggerState();

    boolean canReceiveStagger(DamageContext context);

    void onStaggerStarted();

    void onStaggerRecovered();

    default Color getStaggerThemeColor() {
        return Color.fromRGB(0xFFD54F);
    }

    default String getStaggerDisplayName() {
        return null;
    }

    default double getMaxHealthForStagger() {
        return 0.0;
    }
}
