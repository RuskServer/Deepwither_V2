package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class AuricIngotItem implements CustomItem {

    @Override
    public String getId() {
        return "auric_ingot";
    }

    @Override
    public Material getMaterial() {
        return Material.GOLD_INGOT;
    }

    @Override
    public String getDisplayName() {
        return "§f§lアウリックインゴット";
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
        return "金鉱の原石から不純物を除き、魔導加工に適した硬度へ精錬した金属塊。装備の骨格や導力路に用いられる。";
    }

    @Override
    public double getSellPrice() {
        return 160.0D;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
