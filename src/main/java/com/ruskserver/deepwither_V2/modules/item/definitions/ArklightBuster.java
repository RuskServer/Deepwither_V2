package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ArklightBuster implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public ArklightBuster() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 41.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.8);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 6.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 4.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 160.0);
    }

    @Override
    public String getId() {
        return "arklight_buster";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§f§lAWS-GS07「アークライト・バスター」";
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
        return "ArkWorks Systemsが初期世代に開発した旧式対大型兵装大剣。白基調のフレームと青白く光る導光刃が特徴で、防衛軍の象徴的近接兵装として知られる。";
    }

    @Override
    public int getCustomModelData() {
        return 25;
    }

    @Override
    public double getSellPrice() {
        return 1500.0;
    }

    @Override
    public String getWeaponType() {
        return "大剣";
    }
}
