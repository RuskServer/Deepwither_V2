package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AshTacticalDagger implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AshTacticalDagger() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 20.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 9.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 180.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
        this.baseStats.put(StatType.SPEED, 0.02);
    }

    @Override
    public String getId() {
        return "ash_tactical_dagger";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§f§lLD-TD09a \"Specter Edge / Moonrun Custom\"";
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
        return "Lazward Dynamics製の多目的戦術短刀\"Specter Edge\"の新品個体を、灰機連盟が回収・改修した特別仕様。外観・構造はほぼオリジナルのままだが、内部にLunaris Atelier製の人工アーティファクト《月駆の紋輪》が封入されている。";
    }

    @Override
    public int getCustomModelData() {
        return 8;
    }

    @Override
    public double getSellPrice() {
        return 200.0;
    }

    @Override
    public String getWeaponType() {
        return "ダガー";
    }
}
