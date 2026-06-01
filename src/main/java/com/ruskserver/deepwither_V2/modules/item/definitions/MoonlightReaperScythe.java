package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonlightReaperScythe implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonlightReaperScythe() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 75.0);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 42.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 10.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.MAX_MANA, 80.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.1);
    }

    @Override
    public String getId() {
        return "moonlight_reaper_scythe";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§d§l月影の収穫鎌 ― \"Luna Reaper\"";
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
        return "三層の月影たなびく街に居を構える Lunaris Atelier 製の魔導鎌。低品質なエーテル結晶を独自の魔導研磨技術で再結晶化し、薄く鋭い「魔力波の刃」として定着させている。";
    }

    @Override
    public int getCustomModelData() {
        return 10;
    }

    @Override
    public double getSellPrice() {
        return 16200.0;
    }

    @Override
    public String getWeaponType() {
        return "鎌";
    }
}
