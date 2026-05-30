package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AF07FluxHelmet implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AF07FluxHelmet() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 16.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 14.0);
        this.baseStats.put(StatType.MAX_MANA, 60.0);
    }

    @Override
    public String getId() {
        return "af07_flux_helmet";
    }

    @Override
    public Material getMaterial() {
        return Material.CHAINMAIL_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§d§lAF-07H \"Flux Visor\"";
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
        return "Aetherline Foundryの最新鋭『Flux-Frame』シリーズの頭部ユニット。流体エーテルを充填した多層バイザーは、物理的な衝撃を即座に熱エネルギーへ変換・放散する。中装級の重さを持ちながら、内蔵された姿勢制御アシストにより装着者の首への負担をほぼゼロに抑えている。";
    }
}
