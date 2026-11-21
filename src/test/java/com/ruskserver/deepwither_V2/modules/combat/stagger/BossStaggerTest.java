package com.ruskserver.deepwither_V2.modules.combat.stagger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BossStaggerTest {

    private static StaggerProfile testProfile() {
        return new StaggerProfile(
                1000.0,
                110,
                160,
                60,
                6.0,
                1.6,
                300.0,
                1.0,
                0.85,
                0.75,
                0.5,
                0.04,
                1.5
        );
    }

    @Test
    void staggerTriggersRiposteOnceAndClearsOnRecovery() {
        StaggerProfile profile = testProfile();
        StaggerState state = new StaggerState();

        assertFalse(state.isRiposteAvailable());
        assertFalse(state.consumeRiposte());

        assertFalse(state.add(600.0, profile));
        assertFalse(state.isStaggered());
        assertFalse(state.isRiposteAvailable());

        assertTrue(state.add(400.0, profile));
        assertTrue(state.isStaggered());
        assertTrue(state.isRiposteAvailable());

        // 最初の致命の一撃のみ消費できる
        assertTrue(state.consumeRiposte());
        assertFalse(state.isRiposteAvailable());
        assertFalse(state.consumeRiposte());

        // 崩壊時間経過で復帰
        for (int i = 0; i < profile.staggerDurationTicks() - 1; i++) {
            assertEquals(StaggerState.TickResult.NONE, state.tick(profile));
            assertTrue(state.isStaggered());
        }
        assertEquals(StaggerState.TickResult.RECOVERED, state.tick(profile));
        assertFalse(state.isStaggered());
        assertFalse(state.isRiposteAvailable());
        assertTrue(state.isImmune());
    }

    @Test
    void legacyProfileConstructorProvidesSensibleBreakDefaults() {
        StaggerProfile legacy = new StaggerProfile(
                1500.0, 100, 120, 60, 5.0, 1.5, 300.0, 1.0, 0.85, 0.75, 0.5
        );
        assertEquals(StaggerProfile.DEFAULT_BREAK_BONUS_DAMAGE_RATIO, legacy.breakBonusDamageRatio(), 1e-6);
        assertEquals(StaggerProfile.DEFAULT_RIPOSTE_DAMAGE_MULTIPLIER, legacy.riposteDamageMultiplier(), 1e-6);
    }
}
