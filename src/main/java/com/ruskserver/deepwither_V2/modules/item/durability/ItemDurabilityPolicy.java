package com.ruskserver.deepwither_V2.modules.item.durability;

import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.api.PickaxeItem;
import org.bukkit.Material;

public final class ItemDurabilityPolicy {

    private ItemDurabilityPolicy() {
    }

    public static int getMaxDurability(CustomItem item) {
        if (item.getMaxDurability() >= 0) {
            return item.getMaxDurability();
        }
        if (item instanceof PickaxeItem pickaxe) {
            return Math.max(0, pickaxe.getToolDurability());
        }
        boolean armor = isArmor(item.getMaterial());
        if (!armor && (item.getWeaponType() == null || item.getWeaponType().isBlank())) {
            return 0;
        }
        return armor ? armorDurability(item.getRarity()) : weaponDurability(item.getRarity());
    }

    public static boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }

    private static int weaponDurability(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 180;
            case UNCOMMON -> 300;
            case RARE -> 480;
            case EPIC -> 750;
            case LEGENDARY -> 1_100;
        };
    }

    private static int armorDurability(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 220;
            case UNCOMMON -> 360;
            case RARE -> 600;
            case EPIC -> 900;
            case LEGENDARY -> 1_300;
        };
    }
}
