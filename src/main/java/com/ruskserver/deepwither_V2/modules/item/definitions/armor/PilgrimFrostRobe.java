package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class PilgrimFrostRobe implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public PilgrimFrostRobe() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 50.0);
        this.baseStats.put(StatType.HEALTH, 40.0);
        this.baseStats.put(StatType.MAX_MANA, 100.0);
        this.baseStats.put(StatType.ICE_DAMAGE, 15.0);
    }

    @Override
    public String getId() {
        return "pilgrim_frost_robe";
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§d§l巡礼者の氷衣";
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
    public double getSellPrice() {
        return 15000.0;
    }

    @Override
    public String getFlavorText() {
        return "氷結の巡礼者が纏っていたとされる伝承の衣。氷の魔力が染み込み、身に纏う者に氷属性の加護を与える。";
    }
}
