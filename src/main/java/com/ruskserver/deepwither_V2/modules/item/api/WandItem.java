package com.ruskserver.deepwither_V2.modules.item.api;

import org.bukkit.Particle;

/**
 * このインターフェースを実装したCustomItemは、システムから「魔法の杖」として認識され、
 * 共通の左クリック魔法弾発射機能（WandAttackListener）の対象になります。
 */
public interface WandItem extends CustomItem {

    /**
     * 杖から発射される魔法弾のパーティクル種類を指定します。
     * デフォルトは青い火花（ELECTRIC_SPARK）です。
     */
    default Particle getProjectileParticle() {
        return Particle.ELECTRIC_SPARK;
    }
    
    /**
     * 魔法弾を発射する際のマナ消費量を指定します。
     * デフォルトは 20.0 です。
     */
    default double getManaCost() {
        return 20.0;
    }
    
    /**
     * 魔法弾の弾速（1tickあたりの移動距離ブロック数）を指定します。
     * デフォルトは 1.2 です。
     */
    default double getProjectileSpeed() {
        return 1.2;
    }
    
    /**
     * 魔法弾の最大射程（ブロック数）を指定します。
     * デフォルトは 20.0 です。
     */
    default double getMaxRange() {
        return 20.0;
    }

    /**
     * この魔法の杖から発射される魔法弾に付与される属性タグ（例: "ICE", "FIRE"）を指定します。
     * ここで設定したタグは、DamagePipelineManagerを経由して各アイテムの onAttack/onDefend で判定可能になります。
     */
    default java.util.Set<String> getTags() {
        return java.util.Collections.emptySet();
    }
}
