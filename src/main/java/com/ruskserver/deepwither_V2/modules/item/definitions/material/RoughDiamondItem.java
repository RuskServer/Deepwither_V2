package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class RoughDiamondItem implements CustomItem {

    @Override
    public String getId() {
        return "rough_diamond";
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND;
    }

    @Override
    public String getDisplayName() {
        return "§a§l未研磨のダイヤモンド";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public String getFlavorText() {
        return "鉱床から採れたままの硬質な結晶。研磨前でも、内部から澄んだ光を放っている。";
    }

    @Override
    public double getSellPrice() {
        return 120.0D;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
