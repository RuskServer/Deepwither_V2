package com.ruskserver.deepwither_V2.modules.combat.melee;

import com.ruskserver.deepwither_V2.modules.combat.melee.MeleeGuardStateService.GuardOutcome;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeleeCombatTest {

    @Test
    void weaponTypesMapToExpectedMeleeArchetypes() {
        assertEquals(MeleeWeaponArchetype.SWORD, MeleeWeaponArchetype.fromWeaponType("剣"));
        assertEquals(MeleeWeaponArchetype.SWORD, MeleeWeaponArchetype.fromWeaponType("マチェット"));
        assertEquals(MeleeWeaponArchetype.GREATSWORD, MeleeWeaponArchetype.fromWeaponType("大剣"));
        assertEquals(MeleeWeaponArchetype.AXE, MeleeWeaponArchetype.fromWeaponType("斧"));
        assertEquals(MeleeWeaponArchetype.SPEAR, MeleeWeaponArchetype.fromWeaponType("槍"));
        assertEquals(MeleeWeaponArchetype.SPEAR, MeleeWeaponArchetype.fromWeaponType("ハルバード"));
        assertEquals(MeleeWeaponArchetype.DAGGER, MeleeWeaponArchetype.fromWeaponType("ダガー"));
        assertEquals(MeleeWeaponArchetype.HAMMER, MeleeWeaponArchetype.fromWeaponType("メイス"));
        assertEquals(MeleeWeaponArchetype.HAMMER, MeleeWeaponArchetype.fromWeaponType("ハンマー"));
        assertEquals(MeleeWeaponArchetype.SCYTHE, MeleeWeaponArchetype.fromWeaponType("鎌"));
        assertNull(MeleeWeaponArchetype.fromWeaponType("弓"));
        assertNull(MeleeWeaponArchetype.fromWeaponType("杖"));
    }

    @Test
    void justParryTriggersWithinWindowAndAppliesStaggerAndRiposte() {
        MeleeGuardStateService guard = new MeleeGuardStateService();
        UUID defender = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        long t0 = 10_000L;

        assertTrue(guard.raiseOrRefreshGuard(defender, t0));
        // Hit lands 120ms after raising guard -> JUST PARRY!
        assertEquals(GuardOutcome.JUST_PARRY, guard.evaluateIncomingHit(defender, attacker, t0 + 120L));

        // Attacker is stagger-locked for 900ms and vulnerable to riposte
        assertTrue(guard.isStaggerLocked(attacker, t0 + 500L));
        assertFalse(guard.isStaggerLocked(attacker, t0 + 1100L));
        assertTrue(guard.consumeRiposteVulnerability(attacker, t0 + 800L));
        // Riposte vulnerability is consumed once
        assertFalse(guard.consumeRiposteVulnerability(attacker, t0 + 850L));
    }

    @Test
    void lateGuardHoldBecomesNormalGuardBlock() {
        MeleeGuardStateService guard = new MeleeGuardStateService();
        UUID defender = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        long t0 = 20_000L;

        guard.raiseOrRefreshGuard(defender, t0);
        // Hit lands 350ms after raising guard (past 250ms parry window, within 550ms guard hold)
        assertEquals(GuardOutcome.GUARD_BLOCK, guard.evaluateIncomingHit(defender, attacker, t0 + 350L));
        // Attacker gets short 300ms clash recoil
        assertTrue(guard.isStaggerLocked(attacker, t0 + 500L));
        assertFalse(guard.isStaggerLocked(attacker, t0 + 700L));
        // No riposte window on regular block
        assertFalse(guard.consumeRiposteVulnerability(attacker, t0 + 400L));
    }
}
