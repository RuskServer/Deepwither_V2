package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CelestialWeaveLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CelestialWeaveLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 5.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 25.0);
        this.baseStats.put(StatType.DEFENSE, 5.0);
        this.baseStats.put(StatType.SPEED, 0.006);
    }

    @Override
    public String getId() {
        return "celestial_weave_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§f§l星紡ぎ魔導脚衣「セレスティアル・ウィーブ・レギンス」";
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
        return "魔力流動時の脚部振動を軽減し、詠唱中の移動精度を維持する脚衣。星糸による導路補助と、グール由来の腐灰加工繊維により魔法抵抗を高めている。";
    }
}
