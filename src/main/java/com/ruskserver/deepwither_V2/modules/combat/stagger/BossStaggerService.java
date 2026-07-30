package com.ruskserver.deepwither_V2.modules.combat.stagger;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;

@Service
public class BossStaggerService {

    private final CustomMobManager customMobManager;

    @Inject
    public BossStaggerService(CustomMobManager customMobManager) {
        this.customMobManager = customMobManager;
    }

    public void applyStaggeredDamageMultiplier(DamageContext context) {
        StaggerableBoss boss = resolveBoss(context);
        if (boss == null || !boss.getStaggerState().isStaggered()) {
            return;
        }
        context.multiplyDamage(boss.getStaggerProfile().staggeredDamageMultiplier());
    }

    public void recordResolvedDamage(DamageContext context) {
        StaggerableBoss boss = resolveBoss(context);
        if (boss == null || context.getDamage() <= 0.0 || context.getStaggerMultiplier() <= 0.0
                || !boss.canReceiveStagger(context)) {
            return;
        }

        StaggerProfile profile = boss.getStaggerProfile();
        double typeMultiplier = profile.multiplierFor(context.getType());
        double criticalMultiplier = context.isCritical() ? 1.25 : 1.0;
        double baseAmount = context.getDamage() * typeMultiplier * criticalMultiplier;
        double amount = Math.min(profile.maxStaggerPerHit(), baseAmount)
                * context.getStaggerMultiplier();
        amount = Math.min(profile.maxStagger(), amount);

        if (boss.getStaggerState().add(amount, profile)) {
            boss.onStaggerStarted();
        }
    }

    private StaggerableBoss resolveBoss(DamageContext context) {
        if (context == null || context.getDefender() == null) {
            return null;
        }
        CustomMob customMob = customMobManager.getCustomMob(context.getDefender());
        return customMob instanceof StaggerableBoss boss ? boss : null;
    }
}
