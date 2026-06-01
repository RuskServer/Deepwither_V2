package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CelestialWeaveRHood implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CelestialWeaveRHood() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 7.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 28.0);
        this.baseStats.put(StatType.DEFENSE, 5.0);
    }

    @Override
    public String getId() {
        return "celestial_weave_r_hood";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§b§l星紡ぎ魔導フード・改「セレスティアル・ウィーブ＝アストラ」";
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
    public double getSellPrice() {
        return 800.0;
    }

    @Override
    public String getFlavorText() {
        return "Celest Atelierが星紡ぎ魔導装備を再設計した高純度モデル。星糸導路膜は多層化され、魔法弾生成時のエネルギー散逸を極限まで抑制する。グール由来素材は精製工程を増やすことで腐蝕性を排除し、純粋な耐魔性能へと昇華された。";
    }
}
