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
 * 魔法の杖のサンプル定義。
 * WandItemインターフェースを実装するだけで、WandAttackListenerが左クリックを拾って魔法弾を撃てるようにします。
 */
@Component
public class StarterWand implements WandItem {

    private final Map<StatType, Double> baseStats;

    public StarterWand() {
        this.baseStats = new EnumMap<>(StatType.class);
        // 基本魔法攻撃力25、基本マナ100、攻撃速度(連射速度)1.2回/秒
        this.baseStats.put(StatType.MAGIC_DAMAGE, 25.0);
        this.baseStats.put(StatType.MAX_MANA, 100.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.2);
    }

    @Override
    public String getId() {
        return "starter_wand";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§b駆け出しの杖";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "魔法を学ぶ者が最初に手にする杖。";
    }

    // --- WandItem インターフェースのデフォルトメソッドをオーバーライドしてカスタマイズ可能 ---

    @Override
    public Particle getProjectileParticle() {
        // サンプルなのでデフォルトのELECTRIC_SPARKの代わりにSOUL_FIRE_FLAMEにしてみる
        return Particle.SOUL_FIRE_FLAME;
    }

    @Override
    public double getManaCost() {
        return 15.0; // 少し燃費を良くする
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}
