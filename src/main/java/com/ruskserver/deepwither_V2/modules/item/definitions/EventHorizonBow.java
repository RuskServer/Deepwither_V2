package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class EventHorizonBow implements BowItem {

    private final Map<StatType, Double> baseStats;

    public EventHorizonBow() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 95.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 18.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 250.0);
    }

    @Override
    public String getId() {
        return "event_horizon_bow";
    }

    @Override
    public Material getMaterial() {
        return Material.BOW;
    }

    @Override
    public String getDisplayName() {
        return "§e§l至聖焦弓 ― \"Event Horizon\"";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public String getFlavorText() {
        return "Celest Atelierが「極点への収束」をコンセプトに開発した、最高峰の長距離狙撃兵装。黒磁のエーテル合金で作られた弓身は周囲の光を吸収し、それを弦へと一点に集約させる。";
    }

    @Override
    public int getCustomModelData() {
        return 3;
    }

    @Override
    public double getVelocityMultiplier() {
        return 1.8;
    }

    @Override
    public double getOptimalRange() {
        return 16.0;
    }

    @Override
    public double getMaxDamagePercent() {
        return 1.5;
    }

    @Override
    public String getWeaponType() {
        return "弓";
    }
}
