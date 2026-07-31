package com.ruskserver.deepwither_V2.modules.item.definitions.tool;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.api.PickaxeItem;
import org.bukkit.Material;

import java.util.Set;

/**
 * 旧採掘現場から回収された鉄製ツルハシ。
 */
@Component
public class UsedIronPickaxe implements PickaxeItem {

    @Override
    public String getId() {
        return "used_iron_pickaxe";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_PICKAXE;
    }

    @Override
    public String getDisplayName() {
        return "§7§l中古の鉄ピッケル";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "Lazward Dynamicsの採掘現場で長年使われ、払い下げられた鉄製ピッケル。接合部には多少のがたつきがあるが、鉄材そのものの強度は保たれている。";
    }

    @Override
    public Set<Material> getMineableBlocks() {
        return Set.of(Material.GOLD_ORE, Material.DIAMOND_ORE);
    }

    @Override
    public int getToolDurability() {
        return 250;
    }

    @Override
    public double getSellPrice() {
        return 250.0;
    }
}
