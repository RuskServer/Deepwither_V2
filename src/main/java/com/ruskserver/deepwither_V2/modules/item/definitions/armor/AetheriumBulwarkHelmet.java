package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AetheriumBulwarkHelmet implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AetheriumBulwarkHelmet() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 32.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 28.0);
        this.baseStats.put(StatType.HEALTH, 40.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "aetherium_bulwark_helmet";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§5§lAetherium Bulwark Helmet";
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
    public String getFlavorText() {
        return "Aetherline Foundryが開発した重防御型アーマーシリーズの頭部装甲。超高圧鍛造エーテル鋼を多層構造で積層し、物理・魔法の波動を均一に散らす。その重さは熟練者でなければ首を痛めるほどだが、対価として揺るぎない守護をもたらす。";
    }
}
