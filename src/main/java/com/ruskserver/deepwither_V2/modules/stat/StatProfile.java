package com.ruskserver.deepwither_V2.modules.stat;

import com.ruskserver.deepwither_V2.core.stat.StatType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * エンティティ（プレイヤーやMob）ごとのステータス状態を保持し、
 * 高速な差分(diff)ベースの更新と合計値のキャッシュ計算を行います。
 */
public class StatProfile {

    /**
     * 1つのStatTypeに対する内部状態。
     */
    private static class StatState {
        double sumAdditive = 0.0;
        double sumMultiplicative = 0.0;
        double cachedTotal = 0.0;

        // SourceID -> Modifier
        final Map<String, StatModifier> modifiers = new HashMap<>();

        void recalculateCache() {
            cachedTotal = sumAdditive * (1.0 + sumMultiplicative);
        }
    }

    private final Map<StatType, StatState> states = new EnumMap<>(StatType.class);

    /**
     * モディファイアを追加または上書きします。
     * 古い値との差分のみを計算するため、モディファイアが100個あっても一瞬で更新が終わります。
     *
     * @param type     ステータスの種類
     * @param sourceId 提供元のID (例: "equip_mainhand", "buff_strength")
     * @param value    変動値
     * @param modType  加算か乗算か
     */
    public void setModifier(StatType type, String sourceId, double value, ModifierType modType) {
        StatState state = states.computeIfAbsent(type, k -> new StatState());
        StatModifier oldMod = state.modifiers.get(sourceId);

        // 古い値が存在する場合は、まずそれを合計から減算する
        if (oldMod != null) {
            if (oldMod.getType() == ModifierType.ADDITIVE) {
                state.sumAdditive -= oldMod.getValue();
            } else if (oldMod.getType() == ModifierType.MULTIPLICATIVE) {
                state.sumMultiplicative -= oldMod.getValue();
            }
        }

        // 新しい値を合計に加算する
        StatModifier newMod = new StatModifier(sourceId, value, modType);
        state.modifiers.put(sourceId, newMod);

        if (modType == ModifierType.ADDITIVE) {
            state.sumAdditive += value;
        } else if (modType == ModifierType.MULTIPLICATIVE) {
            state.sumMultiplicative += value;
        }

        // 最終キャッシュ値を更新
        state.recalculateCache();
    }

    /**
     * 指定されたソースのモディファイアを削除します。
     *
     * @param type     ステータスの種類
     * @param sourceId 提供元のID
     */
    public void removeModifier(StatType type, String sourceId) {
        StatState state = states.get(type);
        if (state == null) return;

        StatModifier oldMod = state.modifiers.remove(sourceId);
        if (oldMod != null) {
            if (oldMod.getType() == ModifierType.ADDITIVE) {
                state.sumAdditive -= oldMod.getValue();
            } else if (oldMod.getType() == ModifierType.MULTIPLICATIVE) {
                state.sumMultiplicative -= oldMod.getValue();
            }
            state.recalculateCache();
        }
    }

    /**
     * O(1) で現在のステータス合計値（キャッシュ）を取得します。
     *
     * @param type ステータスの種類
     * @return 合計値
     */
    public double getTotal(StatType type) {
        StatState state = states.get(type);
        return state != null ? state.cachedTotal : 0.0;
    }

    /**
     * O(1) で現在の加算の合計値（ベース値の総和）を取得します。
     * @param type ステータスの種類
     * @return 加算値の総和
     */
    public double getAdditiveSum(StatType type) {
        StatState state = states.get(type);
        return state != null ? state.sumAdditive : 0.0;
    }
}
