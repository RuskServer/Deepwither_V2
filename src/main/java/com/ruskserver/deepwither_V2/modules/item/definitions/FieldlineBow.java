package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class FieldlineBow implements BowItem {

    private final Map<StatType, Double> baseStats;

    public FieldlineBow() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 20.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 150.0);
    }

    @Override
    public String getId() {
        return "fieldline_bow";
    }

    @Override
    public Material getMaterial() {
        return Material.BOW;
    }

    @Override
    public String getDisplayName() {
        return "§9§lLD-B12「フィールドライン・ボウ」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "Lazward Dynamicsが辺境任務向けに量産した軽量フィールドボウ。安価で扱いやすく、輸送兵や傭兵の標準装備として普及している。";
    }

    @Override
    public String getWeaponType() {
        return "弓";
    }
}
