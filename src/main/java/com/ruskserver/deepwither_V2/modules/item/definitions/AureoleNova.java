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
public class AureoleNova implements WandItem {

    private final Map<StatType, Double> baseStats;

    public AureoleNova() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 85.0);
        this.baseStats.put(StatType.LIGHTNING_DAMAGE, 20.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 12.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 220.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.4);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 15.0);
        this.baseStats.put(StatType.MAX_MANA, 150.0);
    }

    @Override
    public String getId() {
        return "aureole_nova";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§e§l至聖光機 ― \"Aureole Nova\"";
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
    public String getFlavorText() {
        return "Celest Atelierの工房主自らが設計した、高出力発射魔法特化型の傑作兵装。黄金のフレームは、内部で発生する膨大な熱と魔力を効率よく導く「加速器」として機能する。「光は等しく降り注ぎ、そして等しく全てを焼き払う」という工房の理念を体現した一品。";
    }

    @Override
    public int getCustomModelData() {
        return 12;
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.END_ROD;
    }

    @Override
    public double getManaCost() {
        return 40.0;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("LIGHTNING");
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}
