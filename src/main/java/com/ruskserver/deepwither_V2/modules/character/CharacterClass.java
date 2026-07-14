package com.ruskserver.deepwither_V2.modules.character;

import org.bukkit.Material;

public enum CharacterClass {
    WARRIOR("戦士", Material.IRON_SWORD),
    MAGE("魔術師", Material.BLAZE_ROD),
    ARCHER("弓使い", Material.BOW),
    HOLY("神聖", Material.GOLDEN_APPLE);

    private final String displayName;
    private final Material icon;

    CharacterClass(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }
}
