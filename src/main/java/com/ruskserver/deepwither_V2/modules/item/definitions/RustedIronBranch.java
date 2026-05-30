package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RustedIronBranch implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RustedIronBranch() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 42.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.6);
    }

    @Override
    public String getId() {
        return "rusted_iron_branch";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§c§l朽鉄の枝";
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
        return "Kryos Industrial Mechanics社製の実戦用マチェットが廃棄・破損しているのを灰機連盟が回収し、内部の欠損していた先進技術を灰機連盟の持つ旧文明のナノマシンを用いて改修した即席改修品。";
    }

    @Override
    public int getCustomModelData() {
        return 11;
    }

    @Override
    public String getWeaponType() {
        return "マチェット";
    }
}
