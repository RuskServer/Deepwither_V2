package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GlacialFortressBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public GlacialFortressBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 36.0);
        this.baseStats.put(StatType.HEALTH, 35.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "glacial_fortress_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§d§lKIM-HG-B08 「Glacial Fortress Boots」";
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
        return "着地時の運動エネルギーを強制的に吸収・放散する超重量ブーツ。高所からの落下すらも「作業の一環」として処理する驚異的な耐衝撃性を誇る。";
    }
}
