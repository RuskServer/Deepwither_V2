package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BastionBreaker implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public BastionBreaker() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 58.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.6);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 4.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 130.0);
    }

    @Override
    public String getId() {
        return "bastion_breaker";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b§lAFS-92『バスティオン・ブレイカー』";
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
        return "Aetherline Foundryが工業合金のみで構築した重量級大剣。魔力をほとんど利用しない代わりに、鍛造圧を極限まで高めた“AF-Std合金刃”を搭載。突出した破壊力と耐久性を誇り、現場部隊の『壊れない大剣』として支持されている。";
    }

    @Override
    public int getCustomModelData() {
        return 9;
    }

    @Override
    public double getSellPrice() {
        return 3200.0;
    }

    @Override
    public String getWeaponType() {
        return "大剣";
    }
}
