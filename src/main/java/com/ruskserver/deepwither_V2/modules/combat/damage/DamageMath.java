package com.ruskserver.deepwither_V2.modules.combat.damage;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PoE / MythicLib 式ダメージ計算の純粋数学ユーティリティ。
 * Bukkit 依存を持たないため、単体テストで厳密に検証できる。
 */
public final class DamageMath {

    public static final double BASE_MAX_RESISTANCE = 75.0;
    public static final double HARD_MAX_RESISTANCE = 90.0;
    public static final double MIN_RESISTANCE = -50.0;
    public static final double MAX_ARMOUR_REDUCTION = 0.90;
    public static final double ARMOUR_CONSTANT = 5.0;
    public static final double BLOCK_DAMAGE_MULTIPLIER = 0.50;

    private DamageMath() {
    }

    /**
     * PoE 式アーマー物理軽減:
     * {@code reduction = effectiveArmour / (effectiveArmour + 5 * rawDamage)}（上限 90%）。
     * 小ダメージは大きく軽減し、一撃が重い攻撃ほど貫通されやすい。
     */
    public static double armourMitigationMultiplier(double armour, double rawPhysicalDamage, double armourPenetration) {
        if (rawPhysicalDamage <= 0.0) return 1.0;
        double effectiveArmour = Math.max(0.0, armour - Math.max(0.0, armourPenetration));
        if (effectiveArmour <= 0.0) return 1.0;
        double reduction = effectiveArmour / (effectiveArmour + ARMOUR_CONSTANT * rawPhysicalDamage);
        reduction = Math.min(MAX_ARMOUR_REDUCTION, Math.max(0.0, reduction));
        return 1.0 - reduction;
    }

    /**
     * PoE 式属性耐性倍率:
     * 上限 {@code min(75 + maxResBonus, 90)} でキャップした後に貫通を差し引く。
     * 耐性 75% → 0.25倍、貫通 15% が入ると実効 60% → 0.40倍。
     */
    public static double elementalResistanceMultiplier(double rawResistance, double maxResBonus, double penetration) {
        double cap = Math.min(HARD_MAX_RESISTANCE, BASE_MAX_RESISTANCE + Math.max(0.0, maxResBonus));
        double capped = Math.min(cap, rawResistance);
        double effective = Math.max(MIN_RESISTANCE, capped - Math.max(0.0, penetration));
        return 1.0 - (effective / 100.0);
    }

    /**
     * 深淵耐性倍率（基本上限 75%、貫通適用可）。
     */
    public static double abyssResistanceMultiplier(double rawResistance, double penetration) {
        double capped = Math.min(BASE_MAX_RESISTANCE, rawResistance);
        double effective = Math.max(MIN_RESISTANCE, capped - Math.max(0.0, penetration));
        return 1.0 - (effective / 100.0);
    }

    /**
     * 一方向ダメージ変換:
     * 指定チャンネル {@code from} のダメージを {@code targetRatios}（0..100%）の割合で各変換先へ振り分ける。
     * 合計が 100% を超える場合は比率を正規化し、元の総ダメージ量を絶対に水増ししない。
     */
    public static void convertChannel(
            EnumMap<DamageChannel, Double> channels,
            DamageChannel from,
            Map<DamageChannel, Double> targetPercents
    ) {
        double source = channels.getOrDefault(from, 0.0);
        if (source <= 0.0 || targetPercents == null || targetPercents.isEmpty()) return;

        double totalPct = 0.0;
        for (Map.Entry<DamageChannel, Double> e : targetPercents.entrySet()) {
            if (e.getKey() != from && e.getValue() != null && e.getValue() > 0.0) {
                totalPct += e.getValue();
            }
        }
        if (totalPct <= 0.0) return;

        double scale = totalPct > 100.0 ? (100.0 / totalPct) : 1.0;
        double convertedSum = 0.0;
        for (Map.Entry<DamageChannel, Double> e : targetPercents.entrySet()) {
            if (e.getKey() == from || e.getValue() == null || e.getValue() <= 0.0) continue;
            double amount = source * (e.getValue() * scale / 100.0);
            if (amount > 0.0) {
                channels.merge(e.getKey(), amount, Double::sum);
                convertedSum += amount;
            }
        }
        double remaining = Math.max(0.0, source - convertedSum);
        if (remaining <= 1e-9) {
            channels.remove(from);
        } else {
            channels.put(from, remaining);
        }
    }

    /**
     * PoE 式エントロピー回避トラッカー。
     * 回避率 50% なら「確実に 2 回に 1 回避ける」挙動になり、理不尽な連続被弾・連続回避を防ぐ。
     */
    public static final class EvasionEntropyTracker {
        private final Map<UUID, Double> entropyMap = new ConcurrentHashMap<>();

        /**
         * @param defenderId     防御者 UUID
         * @param evasionPercent 回避率 (0..90%)
         * @return 回避に成功した場合 true
         */
        public boolean rollEvade(UUID defenderId, double evasionPercent) {
            double clamped = Math.max(0.0, Math.min(90.0, evasionPercent));
            if (clamped <= 0.0) return false;
            double hitChance = 100.0 - clamped;
            double current = entropyMap.getOrDefault(defenderId, 50.0) + hitChance;
            if (current >= 100.0) {
                entropyMap.put(defenderId, current - 100.0);
                return false; // 命中
            }
            entropyMap.put(defenderId, current);
            return true; // 回避
        }

        public void remove(UUID defenderId) {
            entropyMap.remove(defenderId);
        }
    }
}
