package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AccelLapsJacket implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AccelLapsJacket() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 28.0);
        this.baseStats.put(StatType.SPEED, 0.005);
        this.baseStats.put(StatType.ATTACK_SPEED, 8.0);
    }

    @Override
    public String getId() {
        return "accel_laps_jacket";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§d§l戦術機動胸甲「アクセル・ラプス＝ジャケット」";
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
        return "旧来のラプス・ジャケットをネザライト合金で強化しつつ、極限まで肉抜きを施した超軽量装甲。胸部の慣性制御ユニットが急制動による身体への負荷を無効化し、人外の身のこなしを可能にする。";
    }
}
