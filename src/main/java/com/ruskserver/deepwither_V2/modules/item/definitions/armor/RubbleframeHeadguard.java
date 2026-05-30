package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RubbleframeHeadguard implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RubbleframeHeadguard() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 18.0);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public String getId() {
        return "rubbleframe_headguard";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§7§lGFM-EX12-HG「ラブルフレーム・ヘッドガード」";
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
        return "旧文明装甲片を再加工した簡易外骨格頭部ユニット。視界補助は最小限だが、衝撃吸収材と粗雑な補強により物理的には頑丈。";
    }

    @Override
    public int getCustomModelData() {
        return 0;
    }
}
