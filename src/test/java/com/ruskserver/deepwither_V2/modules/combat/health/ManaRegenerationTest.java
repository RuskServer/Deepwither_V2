package com.ruskserver.deepwither_V2.modules.combat.health;

import com.ruskserver.deepwither_V2.modules.combat.CombatStateTracker;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class ManaRegenerationTest {
    @Test
    void combatRecoversTwoPercentAndRestRecoversFivePercent() {
        assertEquals(2, ManaRegeneration.regenerate(0, 100, true), 1e-9);
        assertEquals(5, ManaRegeneration.regenerate(0, 100, false), 1e-9);
        assertEquals(25, ManaRegeneration.regenerate(0, 500, false), 1e-9);
    }

    @Test
    void capsRecoveryAtMaximum() {
        assertEquals(100, ManaRegeneration.regenerate(99, 100, false), 1e-9);
        assertEquals(100, ManaRegeneration.regenerate(99, 100, true), 1e-9);
        assertEquals(100, ManaRegeneration.regenerate(100, 100, false), 1e-9);
    }

    @Test
    void emptyManaFillsInTwentyRestTicks() {
        double mana = 0;
        for (int second = 0; second < 20; second++) mana = ManaRegeneration.regenerate(mana, 100, false);
        assertEquals(100, mana, 1e-9);
    }

    @Test
    void switchesAtTimeoutAndImmediatelyOnNewAttack() {
        AtomicLong now = new AtomicLong();
        CombatStateTracker state = new CombatStateTracker(now::get);
        UUID player = UUID.randomUUID();
        state.recordActivity(player);
        now.set(7_999);
        assertEquals(2, ManaRegeneration.regenerate(0, 100, state.isInCombat(player)), 1e-9);
        now.set(8_000);
        assertEquals(5, ManaRegeneration.regenerate(0, 100, state.isInCombat(player)), 1e-9);
        state.recordActivity(player);
        assertEquals(2, ManaRegeneration.regenerate(0, 100, state.isInCombat(player)), 1e-9);
    }
}
