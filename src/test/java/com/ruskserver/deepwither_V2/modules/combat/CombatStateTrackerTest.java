package com.ruskserver.deepwither_V2.modules.combat;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class CombatStateTrackerTest {
    private final AtomicLong now = new AtomicLong();
    private final CombatStateTracker state = new CombatStateTracker(now::get);
    private final UUID fighter = UUID.randomUUID();
    private final UUID healer = UUID.randomUUID();
    private final UUID boss = UUID.randomUUID();

    @Test
    void expiresExactlyEightSecondsAfterLastAction() {
        assertFalse(state.isInCombat(fighter));
        state.recordActivity(fighter);
        now.set(7_999);
        assertTrue(state.isInCombat(fighter));
        now.set(8_000);
        assertFalse(state.isInCombat(fighter));
    }

    @Test
    void additionalAttackOrPursuitRefreshesDelay() {
        state.recordActivity(fighter);
        now.set(7_000);
        state.recordActivity(fighter);
        now.set(8_000);
        assertTrue(state.isInCombat(fighter));
        now.set(15_000);
        assertFalse(state.isInCombat(fighter));
    }

    @Test
    void supportOfAnIdleAllyDoesNotStartCombat() {
        state.recordSupport(healer, fighter);
        assertFalse(state.isInCombat(healer));
        assertFalse(state.isInCombat(fighter));
    }

    @Test
    void supportStartsCasterTimerWithoutRefreshingTarget() {
        state.recordActivity(fighter);
        now.set(6_000);
        state.recordSupport(healer, fighter);
        now.set(8_000);
        assertFalse(state.isInCombat(fighter));
        assertTrue(state.isInCombat(healer));
        now.set(14_000);
        assertFalse(state.isInCombat(healer));
    }

    @Test
    void selfHealingDoesNotKeepCombatAlive() {
        state.recordActivity(fighter);
        now.set(7_000);
        state.recordSupport(fighter, fighter);
        now.set(8_000);
        assertFalse(state.isInCombat(fighter));
    }

    @Test
    void bossAndItsHealerStayInCombatEvenWithoutFurtherHits() {
        state.joinEncounter(boss, fighter);
        state.recordSupport(healer, fighter);
        now.set(60_000);
        assertTrue(state.isInCombat(fighter));
        assertTrue(state.isInCombat(healer));
        state.endEncounter(boss);
        assertFalse(state.isInCombat(fighter));
        assertFalse(state.isInCombat(healer));
    }

    @Test
    void endingEncounterStillHonorsRecentAttackDelay() {
        state.joinEncounter(boss, fighter);
        state.recordActivity(fighter);
        now.set(1_000);
        state.endEncounter(boss);
        assertTrue(state.isInCombat(fighter));
        now.set(8_000);
        assertFalse(state.isInCombat(fighter));
    }

    @Test
    void endingOneBossDoesNotReleaseAnotherBoss() {
        UUID secondBoss = UUID.randomUUID();
        state.joinEncounter(boss, fighter);
        state.joinEncounter(secondBoss, fighter);
        state.endEncounter(boss);
        assertTrue(state.isInCombat(fighter));
        state.endEncounter(secondBoss);
        assertFalse(state.isInCombat(fighter));
    }

    @Test
    void leavingWorldReleasesOnlyThatParticipantsEncounter() {
        state.joinEncounter(boss, fighter);
        state.joinEncounter(boss, healer);
        state.leaveEncounter(boss, fighter);
        assertFalse(state.isInCombat(fighter));
        assertTrue(state.isInCombat(healer));
    }

    @Test
    void deathOrLogoutClearsTimerAndEncounter() {
        state.joinEncounter(boss, fighter);
        state.recordActivity(fighter);
        state.clearPlayer(fighter);
        assertFalse(state.isInCombat(fighter));
        state.recordSupport(healer, fighter);
        assertFalse(state.isInCombat(healer));
    }

    @Test
    void stopClearsAllState() {
        state.joinEncounter(boss, fighter);
        state.recordActivity(healer);
        state.clear();
        assertFalse(state.isInCombat(fighter));
        assertFalse(state.isInCombat(healer));
    }
}
