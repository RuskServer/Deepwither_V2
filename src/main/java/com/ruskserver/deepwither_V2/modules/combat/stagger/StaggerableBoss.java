package com.ruskserver.deepwither_V2.modules.combat.stagger;

import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;

public interface StaggerableBoss {

    StaggerProfile getStaggerProfile();

    StaggerState getStaggerState();

    boolean canReceiveStagger(DamageContext context);

    void onStaggerStarted();

    void onStaggerRecovered();
}
