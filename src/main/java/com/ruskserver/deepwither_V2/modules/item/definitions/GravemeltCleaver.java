package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GravemeltCleaver implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public GravemeltCleaver() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 45.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 10.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 4.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.5);
    }

    @Override
    public String getId() {
        return "gravemelt_cleaver";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§3§lKIM-AE32「グラヴメルト・クリーバー」";
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
        return "Kryos Industrial Mechanicsが開発した重作業解体用ツールを流用した戦斧。刃部には“KIM魔導合金”として知られるグラヴメルト合金が使われており、エーテル汚染下でも欠けない頑強さを誇る。";
    }

    @Override
    public int getCustomModelData() {
        return 4;
    }

    @Override
    public double getSellPrice() {
        return 4000.0;
    }

    @Override
    public String getWeaponType() {
        return "斧";
    }
}
