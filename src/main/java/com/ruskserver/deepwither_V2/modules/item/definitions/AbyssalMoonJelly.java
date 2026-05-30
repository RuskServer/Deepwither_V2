package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AbyssalMoonJelly implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AbyssalMoonJelly() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 85.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.4);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
    }

    @Override
    public String getId() {
        return "abyssal_moon_jelly";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b深淵の海月";
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
        return "「瞳なき観測者は、万年の孤独を揺らめく。」「青き燐光は、忘れ去られた神々への供物。」「引き抜かれたるは、静寂そのもの。」「深淵の静寂を乱す者に、等しく慈悲を」";
    }

    @Override
    public int getCustomModelData() {
        return 30;
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
