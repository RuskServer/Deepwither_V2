package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AF07FluxLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AF07FluxLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 28.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 24.0);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 8.0);
        this.baseStats.put(StatType.SPEED, 0.002);
    }

    @Override
    public String getId() {
        return "af07_flux_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.CHAINMAIL_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§d§lAF-07L \"Flux Greaves\"";
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
        return 4800.0;
    }

    @Override
    public String getFlavorText() {
        return "人工筋肉とエーテル伝導繊維を組み合わせた、ハイブリッド構造の脚部装甲。激しい戦闘機動においても装甲が干渉せず、装着者の反射神経をダイレクトに運動へと変換する。";
    }
}
