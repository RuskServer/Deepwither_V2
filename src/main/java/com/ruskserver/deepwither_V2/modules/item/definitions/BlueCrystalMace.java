package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BlueCrystalMace implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public BlueCrystalMace() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 65.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 15.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 100.0);
    }

    @Override
    public String getId() {
        return "blue_crystal_mace";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b藍晶のメイス「Blue Crystal Mace」";
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
        return "Lunaris Atelier製の重量級魔導振動メイス。洗練された汎用設計が特徴。扱いには一定の技量が求められるが、物理と魔法が高度に調和したその性能は、探索者から治安部隊まで幅広く支持されている。";
    }

    @Override
    public int getCustomModelData() {
        return 31;
    }

    @Override
    public double getSellPrice() {
        return 5800.0;
    }

    @Override
    public String getWeaponType() {
        return "メイス";
    }
}
