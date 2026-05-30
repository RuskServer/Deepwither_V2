package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class LapsTread implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public LapsTread() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 8.0);
        this.baseStats.put(StatType.SPEED, 0.01);
    }

    @Override
    public String getId() {
        return "laps_tread";
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§b§l機動軽装ブーツ「ラプス・トレッド」";
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
        return "ノア＝セクター外縁部や交易路で活動する輸送兵・傭兵向けに設計された、低コストかつ実用的なLazwardDynamics製軽量戦術ブーツ。防護性能よりも長距離機動性・安定性・耐摩耗性に重点を置いており、都市防衛軍の補給班や民間の軽装部隊にも広く流通している。";
    }
}
