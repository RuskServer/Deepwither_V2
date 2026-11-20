package com.ruskserver.deepwither_V2.modules.combat.melee;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * ガード・ジャストパリィ・弾かれ硬直・リポスト脆弱状態を管理する共有ステートサービス。
 * 循環依存を避けるため独立した {@code @Service} として切り出し、
 * {@code DamagePipelineManager} と {@code MeleeCombatService} の両方から参照する。
 */
@Service
public class MeleeGuardStateService {

    public enum GuardOutcome {
        NONE,
        GUARD_BLOCK,
        JUST_PARRY
    }

    /** ガード開始からジャストパリィが成立する猶予時間 (250ms = 5 ticks) */
    public static final long JUST_PARRY_WINDOW_MILLIS = 250L;
    /** ガード入力 1 回の維持時間（右クリック押しっぱなしで継続更新される） */
    public static final long GUARD_HOLD_REFRESH_MILLIS = 550L;
    /** ガード解除後のパリィ連打防止クールダウン */
    public static final long PARRY_REARM_COOLDOWN_MILLIS = 650L;
    /** 通常ガード時の被ダメージ倍率（65%軽減） */
    public static final double GUARD_DAMAGE_MULTIPLIER = 0.35;
    /** リポスト（パリィ直後の追撃）ダメージ倍率 */
    public static final double RIPOSTE_DAMAGE_MULTIPLIER = 1.60;

    private final Map<UUID, GuardState> guardStates = new ConcurrentHashMap<>();
    private final Map<UUID, Long> staggerLockUntilMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> riposteVulnerableUntilMap = new ConcurrentHashMap<>();
    private BiConsumer<UUID, GuardOutcome> onGuardResolvedListener;
    private BiConsumer<UUID, GuardOutcome> onAttackerRecoiledListener;

    public void setListeners(
            BiConsumer<UUID, GuardOutcome> onGuardResolvedListener,
            BiConsumer<UUID, GuardOutcome> onAttackerRecoiledListener) {
        this.onGuardResolvedListener = onGuardResolvedListener;
        this.onAttackerRecoiledListener = onAttackerRecoiledListener;
    }

    /**
     * 右クリックガードを開始または維持する。
     * @return 新規にガードを構えた（パリィ窓が開いた）場合 true
     */
    public boolean raiseOrRefreshGuard(UUID playerId, long nowMillis) {
        if (isStaggerLocked(playerId, nowMillis)) {
            return false;
        }
        GuardState existing = guardStates.get(playerId);
        if (existing != null && existing.activeUntilMillis >= nowMillis) {
            existing.activeUntilMillis = nowMillis + GUARD_HOLD_REFRESH_MILLIS;
            return false;
        }
        boolean canOpenParryWindow = (existing == null)
                || (nowMillis - existing.lastParryWindowStartMillis >= PARRY_REARM_COOLDOWN_MILLIS);
        long parryStart = canOpenParryWindow ? nowMillis : 0L;
        guardStates.put(playerId, new GuardState(parryStart, nowMillis + GUARD_HOLD_REFRESH_MILLIS));
        return canOpenParryWindow;
    }

    public void clearGuard(UUID playerId) {
        guardStates.remove(playerId);
    }

    public boolean isGuarding(UUID playerId, long nowMillis) {
        GuardState state = guardStates.get(playerId);
        return state != null && state.activeUntilMillis >= nowMillis;
    }

    /**
     * 被弾時に防御者のガード／ジャストパリィを判定する。
     */
    public GuardOutcome evaluateIncomingHit(UUID defenderId, UUID attackerId, long nowMillis) {
        GuardState state = guardStates.get(defenderId);
        if (state == null || state.activeUntilMillis < nowMillis) {
            return GuardOutcome.NONE;
        }

        GuardOutcome outcome;
        if (state.lastParryWindowStartMillis > 0L
                && nowMillis - state.lastParryWindowStartMillis <= JUST_PARRY_WINDOW_MILLIS) {
            outcome = GuardOutcome.JUST_PARRY;
            // パリィ成立時は即座に反撃できるようガードを解除
            guardStates.remove(defenderId);
            if (attackerId != null) {
                applyStaggerLock(attackerId, nowMillis, 900L);
                markRiposteVulnerable(attackerId, nowMillis, 1800L);
            }
        } else {
            outcome = GuardOutcome.GUARD_BLOCK;
            if (attackerId != null) {
                applyStaggerLock(attackerId, nowMillis, 300L);
            }
        }

        if (onGuardResolvedListener != null) {
            onGuardResolvedListener.accept(defenderId, outcome);
        }
        if (attackerId != null && onAttackerRecoiledListener != null) {
            onAttackerRecoiledListener.accept(attackerId, outcome);
        }
        return outcome;
    }

    public void applyStaggerLock(UUID entityId, long nowMillis, long durationMillis) {
        staggerLockUntilMap.put(entityId, Math.max(
                staggerLockUntilMap.getOrDefault(entityId, 0L),
                nowMillis + durationMillis
        ));
    }

    public boolean isStaggerLocked(UUID entityId, long nowMillis) {
        Long until = staggerLockUntilMap.get(entityId);
        if (until == null) return false;
        if (until > nowMillis) return true;
        staggerLockUntilMap.remove(entityId, until);
        return false;
    }

    public void markRiposteVulnerable(UUID entityId, long nowMillis, long durationMillis) {
        riposteVulnerableUntilMap.put(entityId, nowMillis + durationMillis);
    }

    /**
     * 対象がリポスト脆弱状態であれば消費して true を返す。
     */
    public boolean consumeRiposteVulnerability(UUID entityId, long nowMillis) {
        Long until = riposteVulnerableUntilMap.get(entityId);
        if (until == null) return false;
        riposteVulnerableUntilMap.remove(entityId, until);
        return until >= nowMillis;
    }

    public void remove(UUID entityId) {
        guardStates.remove(entityId);
        staggerLockUntilMap.remove(entityId);
        riposteVulnerableUntilMap.remove(entityId);
    }

    private static final class GuardState {
        final long lastParryWindowStartMillis;
        long activeUntilMillis;

        GuardState(long lastParryWindowStartMillis, long activeUntilMillis) {
            this.lastParryWindowStartMillis = lastParryWindowStartMillis;
            this.activeUntilMillis = activeUntilMillis;
        }
    }
}
