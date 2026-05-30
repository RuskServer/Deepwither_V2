package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class LapsStride implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public LapsStride() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 10.0);
        this.baseStats.put(StatType.SPEED, 0.008);
    }

    @Override
    public String getId() {
        return "laps_stride";
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§b§l機動軽装レギンス「ラプス・ストライド」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public String getFlavorText() {
        return "ラプスシリーズの中でも特に\"長時間の歩行・巡察\"に特化したレギンス。膝関節部に低摩擦シェルを採用し、砂地や瓦礫地帯でも足運びの軽さを維持できる。";
    }
}
