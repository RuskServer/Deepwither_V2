package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AF07FluxBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AF07FluxBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 14.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 12.0);
        this.baseStats.put(StatType.SPEED, 0.008);
    }

    @Override
    public String getId() {
        return "af07_flux_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.CHAINMAIL_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§d§lAF-07B \"Flux Treads\"";
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
        return 3200.0;
    }

    @Override
    public String getFlavorText() {
        return "着地の反動をエーテル貯蔵へと回す、高効率なエネルギー循環機構を備えた軍用ブーツ。重量級の防御力を足元に提供しながらも、特殊な靴底の斥力フィールドにより、泥濘や岩場でも軽装のような足取りを可能にする。";
    }
}
