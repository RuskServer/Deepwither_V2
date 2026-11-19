package com.ruskserver.deepwither_V2.modules.combat.damage;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatProfile;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageEngineTest {

    @Test
    void statProfileCombinesAdditiveIncreasedAndMore() {
        StatProfile profile = new StatProfile();
        profile.setModifier(StatType.ATTACK_DAMAGE, "base", 100.0, ModifierType.ADDITIVE);
        profile.setModifier(StatType.ATTACK_DAMAGE, "inc1", 0.50, ModifierType.MULTIPLICATIVE);
        profile.setModifier(StatType.ATTACK_DAMAGE, "inc2", 0.30, ModifierType.MULTIPLICATIVE);
        profile.setModifier(StatType.ATTACK_DAMAGE, "more1", 0.25, ModifierType.MORE);
        profile.setModifier(StatType.ATTACK_DAMAGE, "more2", 0.20, ModifierType.MORE);

        // 100 * (1 + 0.50 + 0.30) * 1.25 * 1.20 = 100 * 1.8 * 1.5 = 270.0
        assertEquals(270.0, profile.getTotal(StatType.ATTACK_DAMAGE), 1e-6);
        assertEquals(0.80, profile.getIncreasedSum(StatType.ATTACK_DAMAGE), 1e-6);
        assertEquals(1.50, profile.getMoreMultiplier(StatType.ATTACK_DAMAGE), 1e-6);
    }

    @Test
    void armourMitigatesSmallHitsMoreThanHeavyHitsAndRespectsPenetration() {
        double armour = 100.0;
        // 100 / (100 + 5 * 10) = 100/150 = 0.6667 reduction -> 0.3333 multiplier
        double smallHitMult = DamageMath.armourMitigationMultiplier(armour, 10.0, 0.0);
        // 100 / (100 + 5 * 100) = 100/600 = 0.1667 reduction -> 0.8333 multiplier
        double heavyHitMult = DamageMath.armourMitigationMultiplier(armour, 100.0, 0.0);
        assertTrue(smallHitMult < heavyHitMult);
        assertEquals(1.0 / 3.0, smallHitMult, 1e-6);
        assertEquals(5.0 / 6.0, heavyHitMult, 1e-6);

        // Penetration removes effective armour
        assertEquals(1.0, DamageMath.armourMitigationMultiplier(armour, 10.0, 120.0), 1e-6);
        // Cap at 90% reduction (0.10 multiplier)
        assertEquals(0.10, DamageMath.armourMitigationMultiplier(1_000_000.0, 1.0, 0.0), 1e-6);
    }

    @Test
    void elementalAndAbyssResistancesRespectCapsAndPenetration() {
        // Base cap 75% -> 0.25x
        assertEquals(0.25, DamageMath.elementalResistanceMultiplier(120.0, 0.0, 0.0), 1e-6);
        // +10% max res -> cap 85% -> 0.15x
        assertEquals(0.15, DamageMath.elementalResistanceMultiplier(120.0, 10.0, 0.0), 1e-6);
        // Hard cap 90% -> 0.10x
        assertEquals(0.10, DamageMath.elementalResistanceMultiplier(120.0, 30.0, 0.0), 1e-6);
        // Penetration applies AFTER cap: 75% capped - 15% pen = 60% -> 0.40x
        assertEquals(0.40, DamageMath.elementalResistanceMultiplier(120.0, 0.0, 15.0), 1e-6);
        // Negative floor -50% -> 1.50x
        assertEquals(1.50, DamageMath.elementalResistanceMultiplier(-20.0, 0.0, 50.0), 1e-6);

        // Abyss resistance cap 75% + penetration
        assertEquals(0.25, DamageMath.abyssResistanceMultiplier(95.0, 0.0), 1e-6);
        assertEquals(0.45, DamageMath.abyssResistanceMultiplier(95.0, 20.0), 1e-6);
    }

    @Test
    void channelConversionNormalizesOver100PercentWithoutInflatingTotal() {
        EnumMap<DamageChannel, Double> channels = new EnumMap<>(DamageChannel.class);
        channels.put(DamageChannel.PHYSICAL, 100.0);

        DamageMath.convertChannel(channels, DamageChannel.PHYSICAL, Map.of(
                DamageChannel.LIGHTNING, 75.0,
                DamageChannel.FIRE, 75.0
        ));

        // 75 + 75 = 150% -> normalized to 50% / 50%, total stays 100
        assertFalse(channels.containsKey(DamageChannel.PHYSICAL));
        assertEquals(50.0, channels.get(DamageChannel.LIGHTNING), 1e-6);
        assertEquals(50.0, channels.get(DamageChannel.FIRE), 1e-6);
    }

    @Test
    void evasionEntropyTrackerDistributesHitsDeterministically() {
        DamageMath.EvasionEntropyTracker tracker = new DamageMath.EvasionEntropyTracker();
        UUID id = UUID.randomUUID();
        int evaded = 0;
        for (int i = 0; i < 10; i++) {
            if (tracker.rollEvade(id, 50.0)) {
                evaded++;
            }
        }
        assertEquals(5, evaded);
    }

    @Test
    void abyssAndTrueChannelsBypassMagicShield() {
        assertTrue(DamageChannel.ABYSS.bypassesMagicShield());
        assertTrue(DamageChannel.TRUE.bypassesMagicShield());
        assertFalse(DamageChannel.PHYSICAL.bypassesMagicShield());
        assertFalse(DamageChannel.FIRE.bypassesMagicShield());
        assertFalse(DamageChannel.ICE.bypassesMagicShield());
        assertFalse(DamageChannel.LIGHTNING.bypassesMagicShield());
        assertFalse(DamageChannel.HOLY.bypassesMagicShield());
    }
}
