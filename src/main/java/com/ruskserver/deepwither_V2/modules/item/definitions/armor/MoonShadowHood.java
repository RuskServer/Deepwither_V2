package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonShadowHood implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonShadowHood() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 5.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 20.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 10.0);
    }

    @Override
    public String getId() {
        return "moon_shadow_hood";
    }

    @Override
    public Material getMaterial() {
        return Material.PAPER;
    }

    @Override
    public String getDisplayName() {
        return "§d§l月影の隠密装束 ― \"Luna Hood\"";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public String getFlavorText() {
        return "月光布のフードに、薄膜状のエーテル結晶を埋め込んだ額冠。装着者の視神経に干渉し、敵が纏う魔力の流れを「刈り取るべき線」として視覚化する。装束全体の魔力バイパスを統合管理する中枢ユニットであり、これ無しでは装束の真の性能を引き出すことはできないとされる。";
    }

    @Override
    public double getSellPrice() {
        return 13200.0;
    }

    @Override
    public int getCustomModelData() {
        return 2;
    }
}
