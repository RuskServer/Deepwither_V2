package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BorealFrameLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public BorealFrameLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 33.0);
        this.baseStats.put(StatType.HEALTH, 25.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "boreal_frame_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§b§lKIM-GS-L03 「Boreal Frame Leggings」";
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
    public String getFlavorText() {
        return "積雪地帯での重機作業脚部フレームを軍用に調整したモデル。高い踏ん張り性能を提供するが、魔法干渉には弱い。";
    }
}
