package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ResidualAshHalberd implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public ResidualAshHalberd() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 45.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 130.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.8);
    }

    @Override
    public String getId() {
        return "residual_ash_halberd";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§c§l残灰のハルバード";
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
        return "旧文明時代のハルバードを、灰機連盟がサルベージし再構築したもの。損傷した部分に魔導合金を流し込み、半生体金属として“再起動”させた兵装。";
    }

    @Override
    public int getCustomModelData() {
        return 21;
    }

    @Override
    public double getSellPrice() {
        return 1600.0;
    }

    @Override
    public String getWeaponType() {
        return "ハルバード";
    }
}
