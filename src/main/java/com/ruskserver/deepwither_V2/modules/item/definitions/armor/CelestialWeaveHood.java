package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CelestialWeaveHood implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CelestialWeaveHood() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 4.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 20.0);
        this.baseStats.put(StatType.DEFENSE, 4.0);
    }

    @Override
    public String getId() {
        return "celestial_weave_hood";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§f§l星紡ぎ魔導フード「セレスティアル・ウィーブ・フード」";
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
        return 200.0;
    }

    @Override
    public String getFlavorText() {
        return "Celest Atelierが魔力射線の乱れを抑制するために開発した魔導フード。内部に組み込まれた\"星糸導路膜\"が魔法弾生成を補助し、出力をわずかに強化する。製造にはグール系個体から採取される異質繊維が用いられ、魔力耐性も向上している。";
    }
}
