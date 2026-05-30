package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CelestialWeaveBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CelestialWeaveBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 4.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 18.0);
        this.baseStats.put(StatType.DEFENSE, 3.0);
        this.baseStats.put(StatType.SPEED, 0.012);
    }

    @Override
    public String getId() {
        return "celestial_weave_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§f§l星紡ぎ魔導靴「セレスティアル・ウィーブ・ブーツ」";
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
    public String getFlavorText() {
        return "魔導衝撃の反発を抑制し、射撃魔術中の立ち位置安定性を高める軽装靴。グール系素材の反魔導繊維が微弱な保護膜を生成し、魔法耐性を底上げする。";
    }
}
