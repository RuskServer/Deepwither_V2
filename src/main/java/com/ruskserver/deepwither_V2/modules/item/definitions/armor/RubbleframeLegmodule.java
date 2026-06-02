package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RubbleframeLegmodule implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RubbleframeLegmodule() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 25.0);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 4.0);
    }

    @Override
    public String getId() {
        return "rubbleframe_legmodule";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§7§lGFM-EX12-LM「ラブルフレーム・レッグモジュール」";
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
    public double getSellPrice() {
        return 800.0;
    }

    @Override
    public String getArmorTrimPattern() {
        return "eye";
    }

    @Override
    public String getArmorTrimMaterial() {
        return "netherite";
    }

    @Override
    public String getFlavorText() {
        return "旧文明残骸フレームの脚部骨格を灰機連盟式に組み替えた外骨格脚部。踏み込み動作を補助する油圧シリンダーにより、近接攻撃の威力がわずかに上昇する。魔法攻撃にはほとんど無防備。";
    }

    @Override
    public int getCustomModelData() {
        return 12;
    }
}
