package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CelestialWeaveRLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CelestialWeaveRLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 9.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 34.0);
        this.baseStats.put(StatType.DEFENSE, 7.0);
        this.baseStats.put(StatType.SPEED, 0.008);
    }

    @Override
    public String getId() {
        return "celestial_weave_r_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§b§l星紡ぎ魔導脚衣・改「セレスティアル・ウィーブ＝アストラ」";
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
        return "高出力詠唱時の身体ブレを抑制するため、星糸導路が脚部全体に配置されている。移動中でも魔法精度を維持できるため、機動射撃魔術との相性が極めて高い。";
    }
}
