package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AccelLapsVisor implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AccelLapsVisor() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 12.0);
        this.baseStats.put(StatType.SPEED, 0.01);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 5.0);
    }

    @Override
    public String getId() {
        return "accel_laps_visor";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§d§l戦術機動ヘルメット「アクセル・ラプス＝バイザー」";
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
        return 2800.0;
    }

    @Override
    public String getFlavorText() {
        return "Lazward Dynamicsの高速戦闘用OSを搭載したラプスシリーズの最新鋭モデル。多機能センサーが敵の予備動作をミリ秒単位で解析し、装着者の回避行動を強力にバックアップする。";
    }
}
