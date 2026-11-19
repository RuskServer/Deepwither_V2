package com.ruskserver.deepwither_V2.modules.stat;

import com.ruskserver.deepwither_V2.core.stat.StatType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * エンティティ（プレイヤーやMob）ごとのステータス状態を保持し、
 * 高速な差分(diff)ベースの更新と合計値のキャッシュ計算を行います。
 * <p>
 * PoE 式スケーリング:
 * {@code Total = sumAdditive * (1.0 + sumMultiplicative[Increased]) * prodMore}
 */
public class StatProfile {

    private static class StatState {
        double sumAdditive = 0.0;
        double sumMultiplicative = 0.0;
        double prodMore = 1.0;
        double cachedTotal = 0.0;

        final Map<String, StatModifier> modifiers = new HashMap<>();

        void recalculateMoreProduct() {
            double prod = 1.0;
            for (StatModifier mod : modifiers.values()) {
                if (mod.getType() == ModifierType.MORE) {
                    prod *= Math.max(0.0, 1.0 + mod.getValue());
                }
            }
            prodMore = prod;
        }

        void recalculateCache() {
            double increased = Math.max(0.0, 1.0 + sumMultiplicative);
            cachedTotal = sumAdditive * increased * prodMore;
        }
    }

    private final Map<StatType, StatState> states = new EnumMap<>(StatType.class);

    public void setModifier(StatType type, String sourceId, double value, ModifierType modType) {
        StatState state = states.computeIfAbsent(type, k -> new StatState());
        StatModifier oldMod = state.modifiers.get(sourceId);
        boolean moreTouched = (modType == ModifierType.MORE);

        if (oldMod != null) {
            if (oldMod.getType() == ModifierType.ADDITIVE) {
                state.sumAdditive -= oldMod.getValue();
            } else if (oldMod.getType() == ModifierType.MULTIPLICATIVE) {
                state.sumMultiplicative -= oldMod.getValue();
            } else if (oldMod.getType() == ModifierType.MORE) {
                moreTouched = true;
            }
        }

        StatModifier newMod = new StatModifier(sourceId, value, modType);
        state.modifiers.put(sourceId, newMod);

        if (modType == ModifierType.ADDITIVE) {
            state.sumAdditive += value;
        } else if (modType == ModifierType.MULTIPLICATIVE) {
            state.sumMultiplicative += value;
        }

        if (moreTouched) {
            state.recalculateMoreProduct();
        }
        state.recalculateCache();
    }

    public void removeModifier(StatType type, String sourceId) {
        StatState state = states.get(type);
        if (state == null) return;

        StatModifier oldMod = state.modifiers.remove(sourceId);
        if (oldMod != null) {
            if (oldMod.getType() == ModifierType.ADDITIVE) {
                state.sumAdditive -= oldMod.getValue();
            } else if (oldMod.getType() == ModifierType.MULTIPLICATIVE) {
                state.sumMultiplicative -= oldMod.getValue();
            } else if (oldMod.getType() == ModifierType.MORE) {
                state.recalculateMoreProduct();
            }
            state.recalculateCache();
        }
    }

    public double getTotal(StatType type) {
        StatState state = states.get(type);
        return state != null ? state.cachedTotal : 0.0;
    }

    public double getAdditiveSum(StatType type) {
        StatState state = states.get(type);
        return state != null ? state.sumAdditive : 0.0;
    }

    public double getIncreasedSum(StatType type) {
        StatState state = states.get(type);
        return state != null ? state.sumMultiplicative : 0.0;
    }

    public double getMoreMultiplier(StatType type) {
        StatState state = states.get(type);
        return state != null ? state.prodMore : 1.0;
    }
}
