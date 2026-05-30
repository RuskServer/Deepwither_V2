package com.ruskserver.deepwither_V2.modules.item.api;

import org.bukkit.Particle;

import java.util.Collections;
import java.util.Set;

/**
 * このインターフェースを実装したCustomItemは、システムから「弓」として認識され、
 * EntityShootBowEvent / EntityDamageByEntityEvent を介して距離減衰ダメージが適用されます。
 */
public interface BowItem extends CustomItem {

    /**
     * 弓から放たれる矢のパーティクル種類を指定します。
     * デフォルトは null（Bukkit標準の矢のまま）。
     */
    default Particle getTrailParticle() {
        return null;
    }

    /**
     * 矢の速度倍率を指定します（1.0 がバニラ標準）。
     */
    default double getVelocityMultiplier() {
        return 1.0;
    }

    /**
     * ダメージが100%になる最適距離（ブロック）。
     */
    default double getOptimalRange() {
        return 8.0;
    }

    /**
     * 近距離での最低ダメージ倍率（0.2 = 20%）。
     */
    default double getMinDamagePercent() {
        return 0.2;
    }

    /**
     * 遠距離での最大ダメージ倍率（1.2 = 120%）。
     */
    default double getMaxDamagePercent() {
        return 1.2;
    }

    /**
     * 距離に応じたダメージ倍率を計算します。
     */
    default double getDamageMultiplier(double distance) {
        double optimal = getOptimalRange();
        double minPct = getMinDamagePercent();
        double maxPct = getMaxDamagePercent();

        if (distance <= 0) return minPct;
        if (distance >= optimal * 2.0) return maxPct;

        if (distance < optimal) {
            // 0 → optimal まで minPct → 1.0 に線形増加
            return minPct + (1.0 - minPct) * (distance / optimal);
        } else {
            // optimal → optimal*2 まで 1.0 → maxPct に線形増加
            return 1.0 + (maxPct - 1.0) * ((distance - optimal) / optimal);
        }
    }

    /**
     * 矢に付与する属性タグ。
     */
    default Set<String> getTags() {
        return Collections.emptySet();
    }
}
