package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AetheriumBulwarkBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AetheriumBulwarkBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 30.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 28.0);
        this.baseStats.put(StatType.HEALTH, 25.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "aetherium_bulwark_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§5§lAetherium Bulwark Boots";
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
    public double getSellPrice() {
        return 6200.0;
    }

    @Override
    public String getCustomArmorAssetId() {
        return "aetherium";
    }

    @Override
    public String getFlavorText() {
        return "着地衝撃を吸収する\"エーテル・ダンパー\"が内蔵された重量型ブーツ。重さゆえに踏み込みの一撃は地面を震わせ、敵の体勢を崩すほどの威圧感を放つ。戦士の歩みを大地に刻む、冷たく硬質な装備。";
    }
}
