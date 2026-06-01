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
public class AstralResonanceRu implements WandItem {

    private final Map<StatType, Double> baseStats;

    public AstralResonanceRu() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 50.0);
        this.baseStats.put(StatType.LIGHTNING_DAMAGE, 4.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 3.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.MAX_MANA, 15.0);
    }

    @Override
    public String getId() {
        return "astral_resonance_ru";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§7§l灰震導杖「アストラル・レゾナンス-RU」";
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
        return "灰機連盟が戦域廃墟から損壊した“アストラル・レゾナンス”を回収し、旧文明残骸パーツと他社汎用導路フレームで強引に再生した再構築モデル。本来の宇宙調律式ハーモニック増幅機構は一部欠損しており、単体出力は原型より明確に低下。";
    }

    @Override
    public int getCustomModelData() {
        return 7;
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.ELECTRIC_SPARK;
    }

    @Override
    public double getManaCost() {
        return 28.0;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("LIGHTNING");
    }

    @Override
    public double getSellPrice() {
        return 1800.0;
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}
