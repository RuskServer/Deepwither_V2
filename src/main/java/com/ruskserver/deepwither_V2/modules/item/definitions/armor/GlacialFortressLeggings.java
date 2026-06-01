package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GlacialFortressLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public GlacialFortressLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 72.0);
        this.baseStats.put(StatType.HEALTH, 50.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "glacial_fortress_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§d§lKIM-HG-L07 「Glacial Fortress Leggings」";
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
    public double getSellPrice() {
        return 8200.0;
    }

    @Override
    public String getFlavorText() {
        return "超重量装甲の自重を支えるべく強化された、産業用アクチュエーター搭載型脚部。圧倒的な質量が装着者を大地に固定し、いかなる衝撃にも揺るがぬ立ち姿を約束する。";
    }
}
