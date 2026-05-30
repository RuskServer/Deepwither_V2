package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonShadowBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonShadowBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 4.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 18.0);
    }

    @Override
    public String getId() {
        return "moon_shadow_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§d§l月影の隠密装束 ― \"Luna Boots\"";
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
        return "足音を消すために靴底へエーテル結晶の粉末を練り込んだ特殊ブーツ。三層の不安定な足場においても、月光が地面を照らすかのように滑らかな移動を約束する。「月影の収穫鎌」を振るう際の遠心力を制御し、打撃へと変換する補助機能を備える。";
    }
}
