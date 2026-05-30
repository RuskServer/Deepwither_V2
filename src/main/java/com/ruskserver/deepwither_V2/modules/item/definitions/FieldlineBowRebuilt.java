package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class FieldlineBowRebuilt implements BowItem {

    private final Map<StatType, Double> baseStats;

    public FieldlineBowRebuilt() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 26.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 10.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 165.0);
    }

    @Override
    public String getId() {
        return "fieldline_bow_rebuilt";
    }

    @Override
    public Material getMaterial() {
        return Material.BOW;
    }

    @Override
    public String getDisplayName() {
        return "§7§lLD-B12R「フィールドライン・リビルト」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "灰機連盟がLD-B12を強引に再構築した危険な改修型。旧文明の残骸から引き剥がしたテンションコイルを移植し、弦圧を原型よりも過剰に強化。出力は向上しているが、振動制御が甘い。";
    }

    @Override
    public String getWeaponType() {
        return "弓";
    }
}
