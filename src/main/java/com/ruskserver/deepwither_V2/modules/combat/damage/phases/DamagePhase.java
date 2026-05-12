package com.ruskserver.deepwither_V2.modules.combat.damage.phases;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;

import java.util.concurrent.ThreadLocalRandom;

/**
 * パイプラインの各処理フェーズを定義するインターフェース。
 */
public interface DamagePhase {
    void process(DamageContext context);

    public static class Base implements DamagePhase {
        private final StatManager statManager;

        public Base(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            if (context.getAttacker() == null) return; // 環境ダメージなど攻撃者がいない場合はスキップ

            // 攻撃者のステータスに基づいて基礎ダメージを上書き・加算
            if (context.getType() == DamageType.PHYSICAL) {
                double physicalAtk = statManager.getTotalStat(context.getAttacker(), StatType.ATTACK_DAMAGE);
                // 基礎ダメージとして設定（バニラの素手ダメージなどは上書きするか加算するかはお好みですが、今回は独自の攻撃力に完全に置き換えるためセットします）
                context.setDamage(physicalAtk > 0 ? physicalAtk : 1.0);
            } else if (context.getType() == DamageType.MAGIC) {
                double magicAtk = statManager.getTotalStat(context.getAttacker(), StatType.MAGIC_DAMAGE);
                context.setDamage(magicAtk > 0 ? magicAtk : 1.0);
            }
        }
    }

    public static class Critical implements DamagePhase {
        private final StatManager statManager;

        public Critical(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            if (context.getAttacker() == null) return;
            // 固定ダメージや環境ダメージではクリティカルは発生しない
            if (context.getType() == DamageType.TRUE_DAMAGE || context.getType() == DamageType.ENVIRONMENTAL) return;

            double critChance = statManager.getTotalStat(context.getAttacker(), StatType.CRITICAL_CHANCE);
            
            // 0.0 ~ 100.0 の間で抽選
            if (critChance > 0 && ThreadLocalRandom.current().nextDouble(100.0) < critChance) {
                context.setCritical(true);
                double critDamageMultiplier = statManager.getTotalStat(context.getAttacker(), StatType.CRITICAL_DAMAGE);
                // デフォルトのクリティカル倍率を1.5倍(150%)とする。ステータスがあればそれに加算
                double finalMultiplier = 1.5 + (critDamageMultiplier / 100.0);
                context.multiplyDamage(finalMultiplier);
            }
        }
    }

    public static class Defense implements DamagePhase {
        private final StatManager statManager;

        public Defense(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            // 固定・環境ダメージは防御力を完全に無視する
            if (context.getType() == DamageType.TRUE_DAMAGE || context.getType() == DamageType.ENVIRONMENTAL) return;

            double defense = 0;
            if (context.getType() == DamageType.PHYSICAL) {
                defense = statManager.getTotalStat(context.getDefender(), StatType.DEFENSE);
            } else if (context.getType() == DamageType.MAGIC) {
                defense = statManager.getTotalStat(context.getDefender(), StatType.MAGIC_DEFENSE);
            }

            if (defense > 0) {
                // 除数250による割合軽減式: ダメージ * (250 / (250 + 防御力))
                double damageReductionMultiplier = 250.0 / (250.0 + defense);
                context.multiplyDamage(damageReductionMultiplier);
            }
        }
    }

    public static class ElementModifier implements DamagePhase {
        private final StatManager statManager;

        public ElementModifier(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            if (context.getAttacker() == null) return;
            if (context.getType() != DamageType.MAGIC) return;

            if (context.hasTag("fire")) {
                double bonus = statManager.getTotalStat(context.getAttacker(), StatType.FIRE_DAMAGE);
                context.multiplyDamage(1.0 + bonus);
            }
            if (context.hasTag("ice")) {
                double bonus = statManager.getTotalStat(context.getAttacker(), StatType.ICE_DAMAGE);
                context.multiplyDamage(1.0 + bonus);
            }
            if (context.hasTag("lightning")) {
                double bonus = statManager.getTotalStat(context.getAttacker(), StatType.LIGHTNING_DAMAGE);
                context.multiplyDamage(1.0 + bonus);
            }
        }
    }
}
