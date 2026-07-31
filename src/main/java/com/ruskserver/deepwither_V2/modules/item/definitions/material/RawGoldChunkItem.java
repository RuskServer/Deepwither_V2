package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class RawGoldChunkItem implements CustomItem {

    @Override
    public String getId() {
        return "raw_gold_chunk";
    }

    @Override
    public Material getMaterial() {
        return Material.RAW_GOLD;
    }

    @Override
    public String getDisplayName() {
        return "§f§l金鉱の原石";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "岩盤から切り出された、金色の鉱脈を含む原石。精錬や加工の素材として利用できる。";
    }

    @Override
    public double getSellPrice() {
        return 40.0D;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
