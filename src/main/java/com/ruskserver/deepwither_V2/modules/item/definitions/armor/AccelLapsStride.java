package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AccelLapsStride implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AccelLapsStride() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 22.0);
        this.baseStats.put(StatType.SPEED, 0.015);
    }

    @Override
    public String getId() {
        return "accel_laps_stride";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§d§l戦術機動レギンス「アクセル・ラプス＝ストライド」";
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
        return 3400.0;
    }

    @Override
    public String getArmorTrimPattern() {
        return "wayfinder";
    }

    @Override
    public String getArmorTrimMaterial() {
        return "diamond";
    }

    @Override
    public String getFlavorText() {
        return "膝関節に最新の低摩擦磁気浮上ギミックを採用した脚部装甲。瓦礫や砂地といった悪路を平地と認識させるほどの踏破能力と、凄まじい瞬発力を装着者に与える。";
    }
}
