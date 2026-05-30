package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CelestialWeaveRTunic implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CelestialWeaveRTunic() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 12.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 40.0);
        this.baseStats.put(StatType.DEFENSE, 8.0);
    }

    @Override
    public String getId() {
        return "celestial_weave_r_tunic";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§b§l星紡ぎ魔導衣・改「セレスティアル・ウィーブ＝アストラ」";
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
        return "星糸導路布を高密度編成した、セレスティアル・ウィーブの中核装備。魔法射出時の初速と収束率を同時に高め、遠距離魔導戦で安定した火力を発揮する。腐灰由来の耐膜は再調整され、強力な属性魔法にも耐えうる防護性能を備える。";
    }
}
