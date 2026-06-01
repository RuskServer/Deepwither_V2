package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RubbleframeFootactuator implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RubbleframeFootactuator() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 20.0);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 3.0);
    }

    @Override
    public String getId() {
        return "rubbleframe_footactuator";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§7§lGFM-EX12-FA「ラブルフレーム・フットアクチュエーター」";
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
    public double getSellPrice() {
        return 800.0;
    }

    @Override
    public String getFlavorText() {
        return "旧文明の補助歩行ユニットを灰機連盟式に再構築した外骨格脚部末端。着地衝撃の吸収と踏破補助により、重量を感じさせない機動性を実現している。";
    }

    @Override
    public int getCustomModelData() {
        return 12;
    }
}
