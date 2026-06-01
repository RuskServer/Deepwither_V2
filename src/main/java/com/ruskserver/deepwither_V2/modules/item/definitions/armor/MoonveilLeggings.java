package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonveilLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonveilLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 22.0);
        this.baseStats.put(StatType.DEFENSE, 4.0);
        this.baseStats.put(StatType.SPEED, 0.005);
    }

    @Override
    public String getId() {
        return "moonveil_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§f§l月紡ぎの脚衣「ルナヴェイル・レギンス」";
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
        return 800.0;
    }

    @Override
    public String getFlavorText() {
        return "側面の魔導糸が移動中の魔法ダメージを緩和する。洞窟都市の魔導職に広く使われている軽装脚衣。";
    }
}
