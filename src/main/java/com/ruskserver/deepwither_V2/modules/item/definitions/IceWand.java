package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.api.WandItem;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class IceWand implements WandItem {

    private final Map<StatType, Double> baseStats;

    public IceWand() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 30.0);
        this.baseStats.put(StatType.MAX_MANA, 150.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
    }

    @Override
    public String getId() {
        return "ice_wand";
    }

    @Override
    public Material getMaterial() {
        return Material.PRISMARINE_SHARD;
    }

    @Override
    public String getDisplayName() {
        return "§b氷の杖";
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
        return "冷気を纏う魔法の杖。氷属性の魔法を放つ。";
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.SNOWFLAKE;
    }

    @Override
    public double getManaCost() {
        return 25.0;
    }

    @Override
    public Set<String> getTags() {
        // "ICE" タグを付与する。これにより、FrostAmulet等のアイテムが氷属性ダメージを増幅できるようになる
        return Collections.singleton("ICE");
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}
