package com.ruskserver.deepwither_V2.modules.combat;

import com.ruskserver.deepwither_V2.modules.combat.shape.ArcShape;
import com.ruskserver.deepwither_V2.modules.combat.shape.HitShape;
import com.ruskserver.deepwither_V2.modules.combat.shape.RayShape;

import java.util.Map;

import static com.ruskserver.deepwither_V2.modules.combat.shape.HitShape.VisualType;

public record WeaponHitProfile(HitShape shape, double baseReach, VisualType visualType) {

    private static final Map<CombatWeaponType, WeaponHitProfile> PROFILES = Map.ofEntries(
            Map.entry(CombatWeaponType.SWORD,      new WeaponHitProfile(new ArcShape(120, 2.0), 3.0, VisualType.SWORD)),
            Map.entry(CombatWeaponType.GREATSWORD, new WeaponHitProfile(new ArcShape(80, 2.5),  4.0, VisualType.SWORD)),
            Map.entry(CombatWeaponType.SPEAR,      new WeaponHitProfile(new RayShape(0.3),       2.0, VisualType.SPEAR)),
            Map.entry(CombatWeaponType.AXE,        new WeaponHitProfile(new ArcShape(50, 3.0),  3.0, VisualType.HEAVY)),
            Map.entry(CombatWeaponType.HALBERD,    new WeaponHitProfile(new ArcShape(60, 2.5),  2.5, VisualType.HEAVY)),
            Map.entry(CombatWeaponType.SCYTHE,     new WeaponHitProfile(new ArcShape(160, 2.0), 2.2, VisualType.SCYTHE)),
            Map.entry(CombatWeaponType.MACE,       new WeaponHitProfile(new ArcShape(30, 3.5),  2.0, VisualType.HEAVY)),
            Map.entry(CombatWeaponType.HAMMER,     new WeaponHitProfile(new ArcShape(30, 4.0),  1.8, VisualType.HEAVY)),
            Map.entry(CombatWeaponType.MACHETE,    new WeaponHitProfile(new ArcShape(40, 2.0),  2.8, VisualType.HEAVY))
    );

    public static WeaponHitProfile from(CombatWeaponType type) {
        return PROFILES.get(type);
    }
}
