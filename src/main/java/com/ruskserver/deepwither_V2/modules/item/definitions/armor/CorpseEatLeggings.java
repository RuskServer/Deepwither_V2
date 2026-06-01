package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CorpseEatLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CorpseEatLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 10.0);
    }

    @Override
    public String getId() {
        return "corpse_eat_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§f§l屍食のレギンス";
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
        return 300.0;
    }

    @Override
    public String getFlavorText() {
        return "LunarisAtelier製の安価なレギンス";
    }
}
