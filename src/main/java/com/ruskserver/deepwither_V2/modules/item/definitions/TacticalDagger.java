package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class TacticalDagger implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public TacticalDagger() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 20.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 180.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
    }

    @Override
    public String getId() {
        return "tactical_dagger";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§f§lLD-TD09 \"Specter Edge\"";
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
        return "Lazward Dynamicsが開発した多目的戦術短刀。旧世代のサバイバルツールを彷彿とさせる見た目だが性能は対異界生物向けに最適化されている。";
    }

    @Override
    public int getCustomModelData() {
        return 8;
    }

    @Override
    public double getSellPrice() {
        return 920.0;
    }

    @Override
    public String getWeaponType() {
        return "ダガー";
    }
}
