package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class HammerMk1 implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public HammerMk1() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 75.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.95);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 3.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.SPEED, -0.02);
    }

    @Override
    public String getId() {
        return "ld_hammer_mk1";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§6§l衝撃戦鎚《ブレイカー・Mk.I》";
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
        return "Lazward Dynamicsが現在の設計思想を確立する以前、都市外縁防衛用として試験的に開発された重量級戦鎚。軽装・機動戦路線とは相反する設計だが、単純明快な破壊力と信頼性の高さから今なお根強い人気を誇る。";
    }

    @Override
    public int getCustomModelData() {
        return 9;
    }

    @Override
    public String getWeaponType() {
        return "ハンマー";
    }
}
