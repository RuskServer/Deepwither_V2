package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * カスタムアイテムのサンプル定義。
 * DIコンテナによって自動収集されるため @Component を付与します。
 */
@Component
public class StarterSword implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public StarterSword() {
        this.baseStats = new EnumMap<>(StatType.class);
        // 基本攻撃力20、基本クリティカル率5%
        this.baseStats.put(StatType.ATTACK_DAMAGE, 20.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
    }

    @Override
    public String getId() {
        return "starter_sword";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§f駆け出しの剣";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "初期装備として支給される標準的な剣です。よく手入れされており、それなりの切れ味を誇りますが、過酷な環境には耐えられないかもしれません。";
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
