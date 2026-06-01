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
public class AfSpreadCaster implements WandItem {

    private final Map<StatType, Double> baseStats;

    public AfSpreadCaster() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 18.0);
        this.baseStats.put(StatType.MAX_MANA, 30.0);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 10.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 4.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 160.0);
        this.baseStats.put(StatType.FIRE_DAMAGE, 8.0);
    }

    @Override
    public String getId() {
        return "af_spread_caster";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§a§lAF-MW08「スプレッド・キャスター」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public String getFlavorText() {
        return "Aetherline Foundryが一般術者向けに量産した範囲制圧用魔導杖。複雑な術式強化機構を排し、魔力を“広く・均等に”拡散させる設計が採用されている。単体火力や爆発的出力には向かないが、集団戦や探索中の雑魚処理では非常に高い安定性を発揮する。";
    }

    @Override
    public int getCustomModelData() {
        return 9;
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.FLAME;
    }

    @Override
    public double getManaCost() {
        return 12.0;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("FIRE");
    }

    @Override
    public double getSellPrice() {
        return 300.0;
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}
