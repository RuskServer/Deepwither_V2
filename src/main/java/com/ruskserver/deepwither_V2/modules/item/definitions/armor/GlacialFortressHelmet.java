package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GlacialFortressHelmet implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public GlacialFortressHelmet() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 38.0);
        this.baseStats.put(StatType.HEALTH, 50.0);
        this.baseStats.put(StatType.SPEED, -0.02);
    }

    @Override
    public String getId() {
        return "glacial_fortress_helmet";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§d§lKIM-HG-H05 「Glacial Fortress Helmet」";
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
    public double getSellPrice() {
        return 6200.0;
    }

    @Override
    public String getFlavorText() {
        return "Boreal Frameの設計を極限まで高密化した、KIM製重工業用外骨格の頭部装甲。内蔵された重力子増幅器が、飛来する物理弾を磁気嵐の如く散らす。";
    }
}
