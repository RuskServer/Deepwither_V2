package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

abstract class AbstractGhoulArmor implements CustomItem {

    private final String id;
    private final String displayName;
    private final Material material;
    private final String flavorText;
    private final String equipmentSetId;
    private final String trimMaterial;
    private final double sellPrice;
    private final Map<StatType, Double> baseStats;

    protected AbstractGhoulArmor(String id, String displayName, Material material, String flavorText,
                                 String equipmentSetId, String trimMaterial, double sellPrice,
                                 Map<StatType, Double> stats) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.flavorText = flavorText;
        this.equipmentSetId = equipmentSetId;
        this.trimMaterial = trimMaterial;
        this.sellPrice = sellPrice;
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.putAll(stats);
    }

    @Override public String getId() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public Material getMaterial() { return material; }
    @Override public String getFlavorText() { return flavorText; }
    @Override public String getEquipmentSetId() { return equipmentSetId; }
    @Override public double getSellPrice() { return sellPrice; }
    @Override public Map<StatType, Double> getBaseStats() { return baseStats; }
    @Override public ItemRarity getRarity() { return ItemRarity.RARE; }
    @Override public String getArmorTrimPattern() { return "rib"; }
    @Override public String getArmorTrimMaterial() { return trimMaterial; }
}
