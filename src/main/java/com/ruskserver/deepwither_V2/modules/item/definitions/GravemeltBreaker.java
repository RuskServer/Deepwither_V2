package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GravemeltBreaker implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public GravemeltBreaker() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 70.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.62);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 18.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 6.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
    }

    @Override
    public String getId() {
        return "gravemelt_breaker";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§d§lKIM-AE52「グラヴメルト・ブレイカー」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public String getFlavorText() {
        return "Kryos Industrial Mechanicsが“純戦闘用”として設計した高純度グラヴメルト合金製戦斧。刃部には重力切断フィールドが常時展開され、装甲や外殻を質量ごと破砕する。";
    }

    @Override
    public int getCustomModelData() {
        return 24;
    }

    @Override
    public String getWeaponType() {
        return "斧";
    }
}
