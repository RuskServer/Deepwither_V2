package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class VariantSpear implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public VariantSpear() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 35.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 100.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.7);
    }

    @Override
    public String getId() {
        return "variant_spear";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§f§lLZ-7「ヴァリアント・スピア」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public String getFlavorText() {
        return "Lazward Dynamics製の多用途スピアで外縁警戒部隊・傭兵向けに大量生産されている。";
    }

    @Override
    public int getCustomModelData() {
        return 3;
    }

    @Override
    public String getWeaponType() {
        return "槍";
    }
}
