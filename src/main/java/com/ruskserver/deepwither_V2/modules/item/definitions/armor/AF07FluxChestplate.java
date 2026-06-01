package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AF07FluxChestplate implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AF07FluxChestplate() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 38.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 32.0);
        this.baseStats.put(StatType.HEALTH, 25.0);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 5.0);
    }

    @Override
    public String getId() {
        return "af07_flux_chestplate";
    }

    @Override
    public Material getMaterial() {
        return Material.CHAINMAIL_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§d§lAF-07C \"Flux Plate\"";
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
        return "重装甲の安心感と軽装の追従性を高次元で融合させたAetherline Foundry製の傑作胸甲。『流体装甲（Flux Plating）』が被弾の瞬間に硬化し、致命的な打撃を分散させる。背面の小型エーテル・バッテリーが動作を補助するため、厚い装甲に反して驚くほど身体が軽い。";
    }
}
