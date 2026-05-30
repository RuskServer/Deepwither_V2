package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonweaveHelmet implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonweaveHelmet() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 6.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 5.0);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 5.0);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 3.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "moonweave_helmet";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§f§l月紡ぎのヘルメット";
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
        return "LunarisAtelier製の洞窟の月光花から抽出した魔素糸を織り込み、魔力の流れを安定させるヘルメット。光なき地に咲く月光花。その糸を紡ぎ、祈りを縫う。";
    }
}
