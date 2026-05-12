package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.api.WandItem;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.EnumMap;
import java.util.Map;

/**
 * 改良魔導杖 ― “Selene Flow Rod” の定義。
 * Lunaris Atelier中級工房製の改良型魔導杖。
 */
@Component
public class SeleneFlowRod implements WandItem {

    private final Map<StatType, Double> baseStats;

    public SeleneFlowRod() {
        this.baseStats = new EnumMap<>(StatType.class);
        // 魔法攻撃力: 14
        this.baseStats.put(StatType.MAGIC_DAMAGE, 14.0);
        // クリティカル率: 6%
        this.baseStats.put(StatType.CRITICAL_CHANCE, 6.0);
        // クリティカルダメージ: 165%
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 165.0);
        // 攻撃速度: 1.1
        this.baseStats.put(StatType.ATTACK_SPEED, 1.1);
        // クールタイム短縮: 3%
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 3.0);
    }

    @Override
    public String getId() {
        return "selene_flow_rod";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§f§l改良魔導杖 ― “Selene Flow Rod”";
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
        return "初級魔導杖“Selene Rod”を基礎に、Lunaris Atelier中級工房が導光管と魔力流路を再設計した改良型。月光樹の枝に薄片状の“月光結晶粉末”を織り込み、魔力の流れを滑らかにすることで詠唱の安定性が飛躍的に向上。";
    }

    @Override
    public int getCustomModelData() {
        return 1;
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.END_ROD;
    }

    @Override
    public double getManaCost() {
        return 12.0;
    }

    @Override
    public double getSellPrice() {
        return 600.0;
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}
