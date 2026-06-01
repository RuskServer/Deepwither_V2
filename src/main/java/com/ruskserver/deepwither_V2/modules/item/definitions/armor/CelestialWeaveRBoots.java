package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CelestialWeaveRBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CelestialWeaveRBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 7.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 26.0);
        this.baseStats.put(StatType.DEFENSE, 5.0);
        this.baseStats.put(StatType.SPEED, 0.015);
    }

    @Override
    public String getId() {
        return "celestial_weave_r_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§b§l星紡ぎ魔導靴・改「セレスティアル・ウィーブ＝アストラ」";
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
        return "星糸と反魔導繊維を複合化した軽装魔導靴。射撃魔術中の踏み込み精度を高め、位置取りのズレを最小限に抑える。";
    }
}
