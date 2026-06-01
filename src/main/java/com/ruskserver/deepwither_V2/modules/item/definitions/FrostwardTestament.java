package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class FrostwardTestament implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public FrostwardTestament() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 92.0);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 25.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.75);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 6.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 130.0);
    }

    @Override
    public String getId() {
        return "kim_frostward_testament";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b§lKS-LG-03 『Frostward Testament』";
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
        return "30年前の戦役で《白氷の聖騎士》が振るったとされるKIM初期世代の大剣。重く、今の基準では扱いづらいが、破壊力は今なお健在。刀身の基部には、聖騎士の祈りを象徴する古式の紋章が薄く刻まれている。";
    }

    @Override
    public int getCustomModelData() {
        return 26;
    }

    @Override
    public double getSellPrice() {
        return 6000.0;
    }

    @Override
    public String getWeaponType() {
        return "大剣";
    }
}
