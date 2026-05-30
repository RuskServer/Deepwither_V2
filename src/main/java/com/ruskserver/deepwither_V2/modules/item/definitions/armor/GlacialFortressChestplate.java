package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GlacialFortressChestplate implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public GlacialFortressChestplate() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 92.0);
        this.baseStats.put(StatType.HEALTH, 60.0);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 14.0);
        this.baseStats.put(StatType.SPEED, -0.02);
    }

    @Override
    public String getId() {
        return "glacial_fortress_chestplate";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§d§lKIM-HG-C06 「Glacial Fortress Chestplate」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public String getFlavorText() {
        return "「氷獄の要塞」の心臓部。数トンの衝撃にも耐えうる多層セルラー装甲を備える。物理的防護に特化した結果、魔法への干渉能力を完全に喪失しているが、その守護は絶対的。";
    }
}
