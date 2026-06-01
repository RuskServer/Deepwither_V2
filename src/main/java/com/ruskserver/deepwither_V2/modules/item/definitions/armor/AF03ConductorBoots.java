package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AF03ConductorBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AF03ConductorBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 6.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 6.0);
        this.baseStats.put(StatType.MAX_MANA, 40.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "af03_conductor_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§f§lAF-03B \"Phase Tread Boots\"";
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
    public double getSellPrice() {
        return 1200.0;
    }

    @Override
    public String getFlavorText() {
        return "魔法と機械技術を併用したAetherline Foundryの代表的なバランス防具。「コンダクタ（導体）」の名の通り、装着者の身体運動・魔力流・外部エネルギーを安定的に\"導く\"ことを目的に設計されている。軽装の機動性と中装の防御力の中間を取り、どの陣営でも扱いやすい標準装備として評価が高い。";
    }
}
