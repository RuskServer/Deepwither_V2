package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class CutAetherDiamondItem implements CustomItem {

    @Override
    public String getId() {
        return "cut_aether_diamond";
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND;
    }

    @Override
    public String getDisplayName() {
        return "§a§l研磨エーテルダイヤ";
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
        return "未研磨の結晶を削り出し、内部のエーテル伝導面を揃えた宝石。高出力装備の魔力中枢に使われる。";
    }

    @Override
    public double getSellPrice() {
        return 300.0D;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
