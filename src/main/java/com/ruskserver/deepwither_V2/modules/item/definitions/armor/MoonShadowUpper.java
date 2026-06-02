package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonShadowUpper implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonShadowUpper() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 8.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 30.0);
        this.baseStats.put(StatType.MAX_MANA, 50.0);
    }

    @Override
    public String getId() {
        return "moon_shadow_upper";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§d§l月影の隠密装束 ― \"Luna Upper\"";
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
    public double getSellPrice() {
        return 16200.0;
    }

    @Override
    public String getCustomArmorAssetId() {
        return "armor3";
    }

    @Override
    public String getFlavorText() {
        return "Lunaris Atelier製の軽量魔導防具。漆黒の月光布で仕立てられている。背面から両腕にかけて微細なエーテル伝導路が刺繍されており、下衣と連結することで、装着者の動作に合わせて魔力を四肢へ供給する。※この装備は同シリーズのレギンスと一対で運用しなければ機能しない。";
    }
}
