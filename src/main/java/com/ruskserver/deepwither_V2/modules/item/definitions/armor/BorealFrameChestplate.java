package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BorealFrameChestplate implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public BorealFrameChestplate() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 42.0);
        this.baseStats.put(StatType.HEALTH, 30.0);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 6.0);
        this.baseStats.put(StatType.SPEED, -0.02);
    }

    @Override
    public String getId() {
        return "boreal_frame_chestplate";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§b§lKIM-GS-C02 「Boreal Frame Chestplate」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public double getSellPrice() {
        return 1800.0;
    }

    @Override
    public String getFlavorText() {
        return "セルラーフレーム構造と低出力重力コアを備えた重量級胴体装甲。物理耐久に優れるが、魔法攻撃への耐性は皆無。";
    }
}
