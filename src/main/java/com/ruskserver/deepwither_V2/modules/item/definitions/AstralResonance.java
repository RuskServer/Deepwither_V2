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
public class AstralResonance implements WandItem {

    private final Map<StatType, Double> baseStats;

    public AstralResonance() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 52.0);
        this.baseStats.put(StatType.LIGHTNING_DAMAGE, 10.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 140.0);
        this.baseStats.put(StatType.MAX_MANA, 25.0);
    }

    @Override
    public String getId() {
        return "astral_resonance";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§b§l星震の導杖「アストラル・レゾナンス」";
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
        return "Harmonics Circleが“宇宙調律理論”研究の成果として開発した純魔力特化導杖。星々から微弱に降り注ぐハーモニック波を抽出し、単体魔法弾の出力を極限まで増幅する。範囲拡散やバースト強化は搭載されていないが、その代償として一点集中の火力は同格帯最高峰。";
    }

    @Override
    public int getCustomModelData() {
        return 7;
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.END_ROD;
    }

    @Override
    public double getManaCost() {
        return 30.0;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("LIGHTNING");
    }

    @Override
    public double getSellPrice() {
        return 12000.0;
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}
