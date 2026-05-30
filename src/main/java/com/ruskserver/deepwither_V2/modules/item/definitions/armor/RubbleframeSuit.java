package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RubbleframeSuit implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RubbleframeSuit() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 35.0);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 10.0);
    }

    @Override
    public String getId() {
        return "rubbleframe_suit";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§7§lGFM-EX12「ラブルフレーム・スーツ」";
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
        return "灰機連盟が旧文明外骨格の残骸を組み直して製造した簡易外骨格スーツ。外部アクチュエーターが打撃動作を補助し、装着者の攻撃力を強化する。魔法的耐性は皆無だが、重量補助機構により機動力ペナルティはない。";
    }
}
