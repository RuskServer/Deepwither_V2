package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class VulcanDriver implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public VulcanDriver() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 95.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.8);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 12.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "amu_vulcan_driver";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§6§l熱核破砕斧《ヴァルカン・ドライバー》";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public String getFlavorText() {
        return "灰機連盟が深層の地熱発電プラントから回収した高出力熱交換器を、無理やり戦斧の心臓部として組み込んだ超重量兵装。刃部からは常に排熱が漏れ出し、まるで溶岩を纏っているかのように赤熱している。";
    }

    @Override
    public int getCustomModelData() {
        return 11;
    }

    @Override
    public double getSellPrice() {
        return 12000.0;
    }

    @Override
    public String getWeaponType() {
        return "斧";
    }
}
