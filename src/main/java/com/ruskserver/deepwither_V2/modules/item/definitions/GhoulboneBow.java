package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GhoulboneBow implements BowItem {

    private final Map<StatType, Double> baseStats;

    public GhoulboneBow() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.RANGED_DAMAGE, 22.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 9.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 155.0);
    }

    @Override
    public String getId() {
        return "ghoulbone_bow";
    }

    @Override
    public Material getMaterial() {
        return Material.BOW;
    }

    @Override
    public String getDisplayName() {
        return "§2§lグールボーン・ボウ";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "グールの骨と腐食した弦で組まれた粗末な弓。Fieldline Bowよりわずかに重い一撃を放つが、扱いはまだ序盤向けに収まっている。";
    }

    @Override
    public double getSellPrice() {
        return 420.0;
    }

    @Override
    public String getWeaponType() {
        return "弓";
    }

    @Override
    public Particle getTrailParticle() {
        return Particle.SMOKE;
    }
}
