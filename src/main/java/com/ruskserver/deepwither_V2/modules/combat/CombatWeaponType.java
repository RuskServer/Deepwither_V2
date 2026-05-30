package com.ruskserver.deepwither_V2.modules.combat;

import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;

import java.util.HashMap;
import java.util.Map;

public enum CombatWeaponType {
    SWORD("剣"),
    GREATSWORD("大剣"),
    SPEAR("槍"),
    AXE("斧"),
    HALBERD("ハルバード"),
    SCYTHE("鎌"),
    MACE("メイス"),
    HAMMER("ハンマー"),
    MACHETE("マチェット"),
    DAGGER("ダガー");

    private final String displayName;

    private static final Map<String, CombatWeaponType> BY_DISPLAY_NAME = new HashMap<>();

    static {
        for (CombatWeaponType type : values()) {
            BY_DISPLAY_NAME.put(type.displayName, type);
        }
    }

    CombatWeaponType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CombatWeaponType fromItem(CustomItem item) {
        if (item == null) return null;
        String weaponType = item.getWeaponType();
        if (weaponType == null) return null;
        return BY_DISPLAY_NAME.get(weaponType);
    }
}
