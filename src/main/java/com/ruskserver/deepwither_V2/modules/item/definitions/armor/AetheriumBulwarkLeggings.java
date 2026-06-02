package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AetheriumBulwarkLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AetheriumBulwarkLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 54.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 51.0);
        this.baseStats.put(StatType.HEALTH, 35.0);
        this.baseStats.put(StatType.SPEED, -0.02);
    }

    @Override
    public String getId() {
        return "aetherium_bulwark_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§5§lAetherium Bulwark Leggings";
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
    public String getCustomArmorAssetId() {
        return "aetherium";
    }

    @Override
    public String getFlavorText() {
        return "Aetherline Foundryが誇る高密度エーテル鋼を脚部に最適化した重量装甲。関節部の\"減衝リング\"により、重装の割には可動性を犠牲にしすぎない。長距離の行軍には不向きだが、戦闘中の耐久能力は同クラスの装備を凌駕する。";
    }
}
