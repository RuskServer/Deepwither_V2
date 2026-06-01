package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ArkEclipse implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public ArkEclipse() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 250.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.5);
    }

    @Override
    public String getId() {
        return "ark_eclipse";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§c§l灰滅斧《アーク・エクリプス》";
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
    public String getFlavorText() {
        return "灰機連盟が回収した旧文明のロストテクノロジーで構成された特大型用の決戦兵装。旧文明のロストテクノロジーであることから一回限りしか使うことはできない。";
    }

    @Override
    public int getCustomModelData() {
        return 1;
    }

    @Override
    public double getSellPrice() {
        return 1500.0;
    }

    @Override
    public String getWeaponType() {
        return "斧";
    }
}
