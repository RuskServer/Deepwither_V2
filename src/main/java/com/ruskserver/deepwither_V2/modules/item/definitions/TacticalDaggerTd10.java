package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class TacticalDaggerTd10 implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public TacticalDaggerTd10() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 48.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 15.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 200.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.2);
        this.baseStats.put(StatType.DEFENSE, 5.0);
    }

    @Override
    public String getId() {
        return "tactical_dagger_td10";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b§lLD-TD10 \"Solidus Edge\"";
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
        return "LD-TD09の運用データを基に、フレーム構造から一新された次世代タクティカルダガー。辺境部隊の精鋭や、高層階層を主戦場とするプロフェッショナル向けに配備される実戦特化モデル。";
    }

    @Override
    public int getCustomModelData() {
        return 28;
    }

    @Override
    public String getWeaponType() {
        return "ダガー";
    }
}
