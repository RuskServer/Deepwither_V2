package com.ruskserver.deepwither_V2.modules.item.api;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;
import java.util.Set;

/**
 * 採掘システムが参照するツルハシ固有の定義。
 */
public interface PickaxeItem extends CustomItem {

    /**
     * @return このツルハシで採掘対象にできるブロック
     */
    Set<Material> getMineableBlocks();

    /**
     * @return 採掘システムで使用する最大耐久値
     */
    int getToolDurability();

    @Override
    default Map<StatType, Double> getBaseStats() {
        return Map.of();
    }

    @Override
    default Set<StatType> getAllowedAddedStats() {
        return Set.of();
    }

    @Override
    default String getWeaponType() {
        return "採掘道具";
    }
}
