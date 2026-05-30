package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class EtherShardHalberd implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public EtherShardHalberd() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 60.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 100.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.8);
    }

    @Override
    public String getId() {
        return "ether_shard_halberd";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b§l残灰の晶戟《エーテル・シャード》";
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
        return "「残灰のハルバード」の欠損した刃部分に、低品質のエーテル結晶を強引に定着させた急造兵装。結晶の不純物が魔力の不規則なスパイクを引き起こし、接触した対象の位相を直接掻き乱す。";
    }

    @Override
    public int getCustomModelData() {
        return 27;
    }

    @Override
    public String getWeaponType() {
        return "ハルバード";
    }
}
