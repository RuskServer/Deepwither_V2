package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ScatteredMoon implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public ScatteredMoon() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 70.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.8);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 4.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
    }

    @Override
    public String getId() {
        return "scattered_moon";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b§lAFS-76『Scattered Moon』";
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
        return "Aetherline Foundryが試験的に開発した重量級シミター《Scattered Moon》。曲刀に質量を与えることで、斬撃時に衝撃を分散させ装甲への干渉力を高める設計が施されている。";
    }

    @Override
    public int getCustomModelData() {
        return 32;
    }

    @Override
    public double getSellPrice() {
        return 4800.0;
    }

    @Override
    public String getWeaponType() {
        return "大剣";
    }
}
