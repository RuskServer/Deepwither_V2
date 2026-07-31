package com.ruskserver.deepwither_V2.modules.item.definitions.tool;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.api.PickaxeItem;
import org.bukkit.Material;

import java.util.Set;

/**
 * Lunaris Atelierが初心者向けに再設計した入門用ツルハシ。
 */
@Component
public class LunarisSurveyPickaxe implements PickaxeItem {

    @Override
    public String getId() {
        return "lunaris_survey_pickaxe";
    }

    @Override
    public Material getMaterial() {
        return Material.STONE_PICKAXE;
    }

    @Override
    public String getDisplayName() {
        return "§f§lLunaris 探鉱ピッケル";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "Lunaris Atelierが新人探索者向けに設計した軽量の探鉱ピッケル。安価な石材へ簡易魔力導路を刻み、鉱脈を傷めず掘り出せるよう調整されている。";
    }

    @Override
    public Set<Material> getMineableBlocks() {
        return Set.of(Material.GOLD_ORE);
    }

    @Override
    public int getToolDurability() {
        return 100;
    }

    @Override
    public double getSellPrice() {
        return 120.0;
    }
}
