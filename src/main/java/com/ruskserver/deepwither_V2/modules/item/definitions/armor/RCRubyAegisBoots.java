package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RCRubyAegisBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RCRubyAegisBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 14.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 26.0);
    }

    @Override
    public String getId() {
        return "rc_ruby_aegis_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.CHAINMAIL_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§4§l紅晶拒界靴《ルビア・エギス》";
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
    public String getFlavorText() {
        return "地面反力と魔力残滓を同時に遮断する特殊靴底。移動性能を犠牲にする代わりに、安定した防御と耐魔性能を得られる設計となっている。";
    }
}
