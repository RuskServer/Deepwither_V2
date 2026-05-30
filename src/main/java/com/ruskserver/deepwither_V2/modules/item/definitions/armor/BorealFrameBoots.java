package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BorealFrameBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public BorealFrameBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 16.0);
        this.baseStats.put(StatType.HEALTH, 15.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "boreal_frame_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§b§lKIM-GS-B04 「Boreal Frame Boots」";
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
    public String getFlavorText() {
        return "落下衝撃の軽減を重視した産業用ブーツを軍事利用したモデル。重量化により機動力は犠牲になっているが、頑強さは十分。";
    }
}
