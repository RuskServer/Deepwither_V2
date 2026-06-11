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
            if (context.getAttacker() == null) return;

            // initialDamage > 0 の場合はスキル/Bow等からの明示的な指定として尊重
            if (context.getDamage() > 0) return;

            if (context.getType() == DamageType.PHYSICAL) {
                double physicalAtk = statManager.getTotalStat(context.getAttacker(), StatType.ATTACK_DAMAGE);
                context.setDamage(physicalAtk > 0 ? physicalAtk : 1.0);
            } else if (context.getType() == DamageType.RANGED) {
                double rangedAtk = statManager.getTotalStat(context.getAttacker(), StatType.RANGED_DAMAGE);
                context.setDamage(rangedAtk > 0 ? rangedAtk : 1.0);
            } else if (context.getType() == DamageType.MAGIC) {
                double magicAtk = statManager.getTotalStat(context.getAttacker(), StatType.MAGIC_DAMAGE);
                context.setDamage(magicAtk > 0 ? magicAtk : 1.0);
            }

            // 距離倍率を適用（弓など）
            if (context.getDistanceMultiplier() != 1.0) {
                context.multiplyDamage(context.getDistanceMultiplier());
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
            if (context.getType() == DamageType.PHYSICAL || context.getType() == DamageType.RANGED) {
                defense = statManager.getTotalStat(context.getDefender(), StatType.DEFENSE);
            } else if (context.getType() == DamageType.MAGIC) {
                defense = statManager.getTotalStat(context.getDefender(), StatType.MAGIC_DEFENSE);
            }

            if (defense > 0) {
                // 除数250による割合軽減式: ダメージ * (250 / (250 + 防御力))
                double damageReductionMultiplier = 250.0 / (250.0 + defense);
                context.multiplyDamage(damageReductionMultiplier);
            }

            // 物理ダメージ追加軽減 (アーティファクトセット効果など)
            if (context.getType() == DamageType.PHYSICAL || context.getType() == DamageType.RANGED) {
                double physReduction = statManager.getTotalStat(context.getDefender(), StatType.PHYSICAL_DAMAGE_REDUCTION);
                if (physReduction > 0) {
                    context.multiplyDamage(1.0 - Math.min(physReduction, 0.9));
                }
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

            // getTotalStat は sumAdditive * (1.0 + sumMultiplicative) の最終値を返すため、
            // 属性値を割合として乗算すると加算値が大きい場合に暴発する。
            // ここでは固定加算として扱い、context.addDamage() で追加する。
            if (context.hasTag("fire")) {
                double bonus = statManager.getTotalStat(context.getAttacker(), StatType.FIRE_DAMAGE);
                context.addDamage(bonus);
            }
            if (context.hasTag("ice")) {
                double bonus = statManager.getTotalStat(context.getAttacker(), StatType.ICE_DAMAGE);
                context.addDamage(bonus);
            }
            if (context.hasTag("lightning")) {
                double bonus = statManager.getTotalStat(context.getAttacker(), StatType.LIGHTNING_DAMAGE);
                context.addDamage(bonus);
            }
        }
    }
}
