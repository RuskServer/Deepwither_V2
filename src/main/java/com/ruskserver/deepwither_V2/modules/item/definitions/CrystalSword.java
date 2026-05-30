package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CrystalSword implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CrystalSword() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 45.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.2);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 5.0);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 6.0);
    }

    @Override
    public String getId() {
        return "crystal_sword";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§d§lCrystal Sword";
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
        return "LunarisAtelier製の魔法戦と近接戦を両立する目的で製造された剣";
    }

    @Override
    public int getCustomModelData() {
        return 6;
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
