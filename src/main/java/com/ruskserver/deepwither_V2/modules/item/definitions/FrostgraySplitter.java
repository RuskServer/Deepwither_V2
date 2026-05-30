package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class FrostgraySplitter implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public FrostgraySplitter() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 62.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.6);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 10.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 142.0);
    }

    @Override
    public String getId() {
        return "frostgray_splitter";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§3§lKIM-GS09「フロストグレイ・スプリッター」";
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
        return "ArkWorks製AWS-GS07を参考に、Kryos Industrial Mechanicsが独自に再設計した重機構大剣。光学導光刃を排し、KIM得意の極低温冷鋼“グレイフロスト合金”を厚く鍛造した結果、圧倒的な質量と破壊力を獲得。";
    }

    @Override
    public int getCustomModelData() {
        return 24;
    }

    @Override
    public String getWeaponType() {
        return "大剣";
    }
}
