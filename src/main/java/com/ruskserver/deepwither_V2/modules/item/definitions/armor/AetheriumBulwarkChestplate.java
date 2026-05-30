package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AetheriumBulwarkChestplate implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AetheriumBulwarkChestplate() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 68.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 62.0);
        this.baseStats.put(StatType.HEALTH, 40.0);
        this.baseStats.put(StatType.SPEED, -0.02);
    }

    @Override
    public String getId() {
        return "aetherium_bulwark_chestplate";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§5§lAetherium Bulwark Chestplate";
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
        return "Aetherline Foundryの象徴ともいえる重装胸甲。「均衡炉心（Balance Core）」と呼ばれるエーテル共振板を内蔵し、あらゆる衝撃を鈍化させる。着用者は圧迫感すら感じるほどの重厚な防護に包まれ、戦場での生存率は桁外れに高い。";
    }
}
