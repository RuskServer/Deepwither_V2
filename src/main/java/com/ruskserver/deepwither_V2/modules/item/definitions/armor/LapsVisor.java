package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class LapsVisor implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public LapsVisor() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 6.0);
        this.baseStats.put(StatType.SPEED, 0.005);
    }

    @Override
    public String getId() {
        return "laps_visor";
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§b§l機動軽装ヘルメット「ラプス・バイザー」";
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
        return "軽量化と視界確保を最優先して設計された、LazwardDynamics製の戦術用ヘルメット。ノア＝セクター外縁での長距離移動を伴う任務に適し、最小限の装甲ながら高い通気性と通信補助システムを備える。";
    }
}
