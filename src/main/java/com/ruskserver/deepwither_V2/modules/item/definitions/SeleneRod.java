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
 * 初級魔導杖 ― “Selene Rod” の定義。
 * Lunaris Atelier製の入門用魔導杖。
 */
@Component
public class SeleneRod implements WandItem {

    private final Map<StatType, Double> baseStats;

    public SeleneRod() {
        this.baseStats = new EnumMap<>(StatType.class);
        // 魔法攻撃力: 10
        this.baseStats.put(StatType.MAGIC_DAMAGE, 10.0);
        // クリティカル率: 5%
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        // クリティカルダメージ: 150%
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 150.0);
        // 攻撃速度: 1.0
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
        // クールタイム短縮: 2%
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 2.0);
    }

    @Override
    public String getId() {
        return "selene_rod";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§f§l初級魔導杖 ― “Selene Rod”";
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
        return "月光樹の若枝を基に、Lunaris Atelierの見習い職人が仕上げた入門用魔導杖。素材の純度は高くないが、魔力伝導路の設計は上位モデルと同じ思想で作られており、安定した詠唱補助と魔力回路の安全制御を両立している。";
    }

    @Override
    public int getCustomModelData() {
        return 1;
    }

    @Override
    public Particle getProjectileParticle() {
        // 月光をイメージした白い輝き
        return Particle.END_ROD;
    }

    @Override
    public double getManaCost() {
        // 入門用として扱いやすい低コスト設定
        return 10.0;
    }

    @Override
    public double getSellPrice() {
        return 250.0;
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}
