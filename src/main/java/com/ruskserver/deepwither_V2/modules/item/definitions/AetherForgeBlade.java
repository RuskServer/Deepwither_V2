package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AetherForgeBlade implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AetherForgeBlade() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 20.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
    }

    @Override
    public String getId() {
        return "aether_forge_blade";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§f§lAether Forge Blade";
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
    public String getFlavorText() {
        return "Aether Edgeシリーズの廉価系として最も普及しているモデルだが、新合金「フォージ・コンポジット」を採用することで耐久性が大幅に向上した“しぶとく壊れない”エントリー剣。";
    }

    @Override
    public int getCustomModelData() {
        return 22;
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
