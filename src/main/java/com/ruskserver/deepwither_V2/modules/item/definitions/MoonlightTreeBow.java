package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonlightTreeBow implements BowItem {

    private final Map<StatType, Double> baseStats;

    public MoonlightTreeBow() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 50.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 12.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 150.0);
    }

    @Override
    public String getId() {
        return "moonlight_tree_bow";
    }

    @Override
    public Material getMaterial() {
        return Material.BOW;
    }

    @Override
    public String getDisplayName() {
        return "§c§l月光樹";
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
        return "Lunaris Atelierが“魔力と生命を融合させた素材”の実験過程で生まれた、生体魔導弓。月光を受けることで弓そのものが脈動し、矢を“放つ”のではなく“送り出す”。";
    }

    @Override
    public int getCustomModelData() {
        return 1;
    }

    @Override
    public String getWeaponType() {
        return "弓";
    }
}
