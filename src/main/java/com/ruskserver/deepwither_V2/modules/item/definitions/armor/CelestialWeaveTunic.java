package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CelestialWeaveTunic implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CelestialWeaveTunic() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 7.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 32.0);
        this.baseStats.put(StatType.DEFENSE, 6.0);
    }

    @Override
    public String getId() {
        return "celestial_weave_tunic";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§f§l星紡ぎ魔導衣「セレスティアル・ウィーブ・チュニック」";
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
    public double getSellPrice() {
        return 200.0;
    }

    @Override
    public String getFlavorText() {
        return "Celest Atelierが遠距離魔導戦に特化して設計した主装衣。\"星紡ぎ導路布\"が魔導射出の初速を安定化させ、魔法火力を底上げする。胸部にはグール系素材由来の腐蝕耐膜が組み込まれており、魔法属性耐性に優れる。";
    }
}
