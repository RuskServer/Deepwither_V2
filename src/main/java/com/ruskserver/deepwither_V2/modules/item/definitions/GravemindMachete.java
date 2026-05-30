package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GravemindMachete implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public GravemindMachete() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 60.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.65);
    }

    @Override
    public String getId() {
        return "gravemind_machete";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b§lKIM-GCM41「グレイヴマインド・マチェット」";
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
        return "Kryos Industrial Mechanicsが製造した重力制御式実戦マチェット。刃の根元に搭載された「重力共振モジュール」が局所重力場を発生させ、エーテル切断機構と連動して桁違いの切断効率を発揮する。";
    }

    @Override
    public int getCustomModelData() {
        return 11;
    }

    @Override
    public String getWeaponType() {
        return "マチェット";
    }
}
