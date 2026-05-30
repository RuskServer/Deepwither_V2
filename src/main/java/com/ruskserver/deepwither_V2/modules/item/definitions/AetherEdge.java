package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AetherEdge implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AetherEdge() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 17.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 6.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
    }

    @Override
    public String getId() {
        return "aether_edge";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§f§lAether Edge";
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
        return "AetherlineFoundry製の最もベーシックで象徴的な剣";
    }

    @Override
    public int getCustomModelData() {
        return 3;
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
