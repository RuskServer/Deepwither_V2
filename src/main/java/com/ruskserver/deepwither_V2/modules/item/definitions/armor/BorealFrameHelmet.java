package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BorealFrameHelmet implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public BorealFrameHelmet() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 18.0);
        this.baseStats.put(StatType.HEALTH, 30.0);
        this.baseStats.put(StatType.SPEED, -0.02);
    }

    @Override
    public String getId() {
        return "boreal_frame_helmet";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§b§lKIM-GS-H01 「Boreal Frame Helmet」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public double getSellPrice() {
        return 1600.0;
    }

    @Override
    public String getFlavorText() {
        return "極寒環境作業用ヘルムを軍事転用したKryos Industrial Mechanics製重装頭部装甲。簡易重力パネルが衝撃を散らすが、魔法には脆弱。";
    }
}
