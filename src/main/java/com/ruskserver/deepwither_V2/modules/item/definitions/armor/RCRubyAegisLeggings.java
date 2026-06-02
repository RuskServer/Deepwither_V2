package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RCRubyAegisLeggings implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RCRubyAegisLeggings() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 18.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 34.0);
        this.baseStats.put(StatType.SPEED, -0.01);
    }

    @Override
    public String getId() {
        return "rc_ruby_aegis_leggings";
    }

    @Override
    public Material getMaterial() {
        return Material.CHAINMAIL_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§4§l紅晶拒界脚甲《ルビア・エギス》";
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
    public double getSellPrice() {
        return 8000.0;
    }

    @Override
    public String getCustomArmorAssetId() {
        return "armor2";
    }

    @Override
    public String getFlavorText() {
        return "脚部への物理衝撃を分散するエーテル合金骨格。ルビー系遮断結晶が魔力干渉を抑制し、詠唱妨害や拘束魔法への耐性を高めている。";
    }
}
