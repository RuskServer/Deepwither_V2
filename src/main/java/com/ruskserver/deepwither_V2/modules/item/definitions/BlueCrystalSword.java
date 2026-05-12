package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

/**
 * 藍晶の剣「Blue Crystal Sword」の定義。
 * Lunaris Atelier製の入門用魔剣。
 */
@Component
public class BlueCrystalSword implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public BlueCrystalSword() {
        this.baseStats = new EnumMap<>(StatType.class);
        // 物理攻撃力: 20
        this.baseStats.put(StatType.ATTACK_DAMAGE, 20.0);
        // 魔法攻撃力: 8
        this.baseStats.put(StatType.MAGIC_DAMAGE, 8.0);
        // クリティカル率: 6%
        this.baseStats.put(StatType.CRITICAL_CHANCE, 6.0);
        // クリティカルダメージ: 100%
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 100.0);
        // 攻撃速度: 1.2
        this.baseStats.put(StatType.ATTACK_SPEED, 1.2);
    }

    @Override
    public String getId() {
        return "blue_crystal_sword";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b藍晶の剣「Blue Crystal Sword」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public String getFlavorText() {
        return "Lunaris Atelierの見習い職人が仕上げた入門用の魔剣。素材の質は良くないが安定した戦闘能力を持っており若干の魔法攻撃にも適している。";
    }

    @Override
    public int getCustomModelData() {
        return 29;
    }

    @Override
    public double getSellPrice() {
        return 400.0;
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
