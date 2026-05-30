package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonShadowLower implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonShadowLower() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 6.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 24.0);
        this.baseStats.put(StatType.SPEED, 0.01);
    }

    @Override
    public String getId() {
        return "moon_shadow_lower";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_LEGGINGS;
    }

    @Override
    public String getDisplayName() {
        return "§d§l月影の隠密装束 ― \"Luna Lower\"";
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
        return "「月影の隠密装束」の脚部ユニット。上衣からの魔導信号を受け取り、踏み込みの瞬間に位相をわずかにずらすことで、慣性を無視した移動を可能にする。単体では魔導回路が閉じておらず、マナの漏出を引き起こすため、必ず上衣とセットで着用することが推奨されている。";
    }
}
