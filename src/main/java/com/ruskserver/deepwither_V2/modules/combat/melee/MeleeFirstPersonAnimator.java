package com.ruskserver.deepwither_V2.modules.combat.melee;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft 26.3 のネイティブ {@code display_context: firstperson_righthand} ＋
 * {@code custom_model_data.strings} ＋ {@code attack_animation / interact_animation = NONE (0)} を使って、
 * Paper 26.3 ネイティブ API のみで確実に毎 tick（20fps・32フレーム区分フェーズ同期・ヒットストップ停止対応）の
 * 一人称武器アニメーションを再生する。
 * <p>
 * ※ PacketEvents の {@code SpigotConversionUtil.fromBukkitItemStack} は Paper 26.3 の新コンポーネントで
 * 変換失敗するため、100% 確実に動作する Paper ネイティブ API で駆動する。
 */
public final class MeleeFirstPersonAnimator {

    public static final int SWING_FRAME_COUNT = 32;
    public static final int GUARD_FRAME_COUNT = 8;
    public static final int PARRY_FRAME_COUNT = 12;
    public static final int RECOIL_FRAME_COUNT = 16;

    private static final double PHASE_WINDUP_END = 0.30;
    private static final double PHASE_ACTIVE_END = 0.56;

    private static final SwingAnimation NO_SWING = SwingAnimation.swingAnimation()
            .type(SwingAnimation.Animation.NONE)
            .duration(0)
            .build();

    private final Map<UUID, PlaybackState> playbacks = new ConcurrentHashMap<>();

    public void start() {
        // メインスレッド tick() から駆動するため外部スレッド不要
    }

    public void stop() {
        for (PlaybackState state : playbacks.values()) {
            if (state.player.isOnline()) {
                clearPoseOnSlot(state.player, state.heldSlot);
            }
        }
        playbacks.clear();
    }

    /**
     * 武器を手に持った瞬間に一度だけ {@code attack_animation / interact_animation = NONE (0)} を適用し、
     * 攻撃初撃時のコンポーネント差分による持ち替えボブやバニラ腕振りを防ぐ。
     */
    public static void prepareWeaponFor1P(Player player, int slot) {
        if (player == null || !player.isOnline()) return;
        if (slot < 0 || slot >= player.getInventory().getSize()) return;
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack == null || stack.getType().isAir()) return;
        boolean changed = ensureNoVanillaSwing(stack);
        if (applyPoseToStack(stack, null)) {
            changed = true;
        }
        if (changed) {
            player.getInventory().setItem(slot, stack);
        }
    }

    /**
     * 一人称アニメーションシーケンスを開始する（メインスレッド専用）。
     */
    public void playSequence(Player player, String sequenceName, int frameCount, long durationMillis, boolean holdLastFrame) {
        long safeDur = Math.max(50L, durationMillis);
        playInternal(player, sequenceName, frameCount, safeDur, -1L, -1L, holdLastFrame);
    }

    /**
     * Windup / Active / Recovery の各フェーズ長に合わせて 1P モーションを区分線形同期再生する。
     * これにより攻撃速度や武器種が変わっても、Active 開始（VFX 発生）と 1P 溜め頂点（0.30）、
     * Active 終了（VFX・叩きつけ着弾）と 1P 振り抜き終端（0.56）が完全に一致する。
     */
    public void playPhasedSwing(Player player, String sequenceName, int frameCount,
                                long windupMs, long activeMs, long recoveryMs) {
        long w = Math.max(50L, windupMs);
        long a = Math.max(50L, activeMs);
        long r = Math.max(50L, recoveryMs);
        playInternal(player, sequenceName, frameCount, w + a + r, w, w + a, false);
    }

    private void playInternal(Player player, String sequenceName, int frameCount,
                              long totalDurationMs, long windupEndMs, long activeEndMs, boolean holdLastFrame) {
        if (player == null || !player.isOnline()) return;
        int heldSlot = player.getInventory().getHeldItemSlot();
        ItemStack held = player.getInventory().getItem(heldSlot);
        if (held == null || held.getType().isAir()) return;

        int safeFrames = Math.max(1, frameCount);
        long nowNanos = System.nanoTime();
        long durNanos = Math.max(50_000_000L, totalDurationMs * 1_000_000L);
        long windupEndNanos = windupEndMs > 0 ? windupEndMs * 1_000_000L : -1L;
        long activeEndNanos = activeEndMs > 0 ? activeEndMs * 1_000_000L : -1L;

        PlaybackState state = new PlaybackState(player, heldSlot, sequenceName, safeFrames,
                nowNanos, durNanos, windupEndNanos, activeEndNanos, holdLastFrame);
        playbacks.put(player.getUniqueId(), state);

        // フレーム 0 を即適用して遅延ゼロで反応させる
        state.lastSentFrame = 0;
        applyPoseToHeldSlot(player, heldSlot, sequenceName + "_0");
    }

    /**
     * 毎サーバー tick（メインスレッド）で呼び出し、全プレイヤーの 1P フレームを進行させる。
     */
    public void tick() {
        long now = System.nanoTime();
        for (Map.Entry<UUID, PlaybackState> entry : playbacks.entrySet()) {
            PlaybackState state = entry.getValue();
            if (!state.player.isOnline()) {
                playbacks.remove(entry.getKey(), state);
                continue;
            }
            if (state.paused) {
                continue;
            }
            long elapsed = now - state.startNanos - state.pausedDurationNanos;
            double progress = state.resolveProgress(elapsed);
            int frameIdx = (int) Math.round(progress * (state.frameCount - 1));
            if (frameIdx != state.lastSentFrame) {
                state.lastSentFrame = frameIdx;
                applyPoseToHeldSlot(state.player, state.heldSlot, state.sequenceName + "_" + frameIdx);
            }
            if (elapsed >= state.durationNanos && !state.holdLastFrame) {
                playbacks.remove(entry.getKey(), state);
                clearPoseOnSlot(state.player, state.heldSlot);
            }
        }
    }

    /**
     * ヒットストップ中は 1P タイマーを一時停止して刃先をピタッと止める。
     */
    public void setPaused(UUID playerId, boolean paused) {
        PlaybackState state = playbacks.get(playerId);
        if (state == null) return;
        long now = System.nanoTime();
        if (paused && !state.paused) {
            state.paused = true;
            state.pauseStartNanos = now;
        } else if (!paused && state.paused) {
            state.pausedDurationNanos += Math.max(0L, now - state.pauseStartNanos);
            state.paused = false;
        }
    }

    /**
     * 再生中の一人称アニメーションを終了し、待機姿勢（元の武器モデル）へ即復元する。
     */
    public void stopAndRestore(Player player) {
        if (player == null) return;
        PlaybackState removed = playbacks.remove(player.getUniqueId());
        if (removed != null && player.isOnline()) {
            clearPoseOnSlot(player, removed.heldSlot);
        }
    }

    /**
     * ログアウト時に再生エントリを破棄する。
     */
    public void cancelWithoutRestore(UUID playerId) {
        playbacks.remove(playerId);
    }

    private static void applyPoseToHeldSlot(Player player, int slot, String poseId) {
        if (player == null || !player.isOnline()) return;
        if (slot < 0 || slot >= player.getInventory().getSize()) return;
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack == null || stack.getType().isAir()) return;
        boolean changed = ensureNoVanillaSwing(stack);
        if (applyPoseToStack(stack, poseId) || changed) {
            player.getInventory().setItem(slot, stack);
        }
    }

    private static boolean ensureNoVanillaSwing(ItemStack stack) {
        boolean changed = false;
        SwingAnimation atk = stack.getData(DataComponentTypes.ATTACK_ANIMATION);
        if (atk == null || atk.type() != SwingAnimation.Animation.NONE || atk.duration() != 0) {
            stack.setData(DataComponentTypes.ATTACK_ANIMATION, NO_SWING);
            changed = true;
        }
        SwingAnimation inter = stack.getData(DataComponentTypes.INTERACT_ANIMATION);
        if (inter == null || inter.type() != SwingAnimation.Animation.NONE || inter.duration() != 0) {
            stack.setData(DataComponentTypes.INTERACT_ANIMATION, NO_SWING);
            changed = true;
        }
        return changed;
    }

    public static void clearPoseOnSlot(Player player, int slot) {
        if (player == null || slot < 0 || slot >= player.getInventory().getSize()) return;
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack == null || stack.getType().isAir()) return;
        if (applyPoseToStack(stack, null)) {
            player.getInventory().setItem(slot, stack);
        }
    }

    public static boolean clearPoseOnStack(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        return applyPoseToStack(stack, null);
    }

    private static boolean applyPoseToStack(ItemStack stack, String poseId) {
        CustomModelData existing = stack.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        List<String> currentStrings = existing != null ? existing.strings() : List.of();
        String currentPose = currentStrings.isEmpty() ? null : currentStrings.get(0);

        String targetPose = (poseId == null || poseId.isBlank()) ? null : poseId;
        if (java.util.Objects.equals(currentPose, targetPose)) {
            return false;
        }

        CustomModelData.Builder builder = CustomModelData.customModelData();
        if (existing != null) {
            for (Float f : existing.floats()) builder.addFloat(f);
            for (Boolean flag : existing.flags()) builder.addFlag(flag);
            for (Color c : existing.colors()) builder.addColor(c);
        }
        if (targetPose != null) {
            builder.addString(targetPose);
        }
        stack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, builder.build());
        return true;
    }

    /**
     * コンボステップに対応する一人称アニメーションシーケンス名を返す。
     */
    public static String swingSequenceName(MeleeWeaponArchetype archetype, int stepIndex, MeleeWeaponArchetype.ComboStep step) {
        if (step.thrust()) return "thrust";
        if (step.groundShockwave()) return "slam";
        return (stepIndex % 2 == 1) ? "slash_lr" : "slash_rl";
    }

    /**
     * 進行度 (0.0..1.0) から対象シーケンスのフレーム ID（例: {@code slash_rl_3}）を生成する。
     */
    public static String frameId(String sequenceName, double progress, int totalFrames) {
        int clamped = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * (totalFrames - 1));
        return sequenceName + "_" + clamped;
    }

    private static final class PlaybackState {
        final Player player;
        final int heldSlot;
        final String sequenceName;
        final int frameCount;
        final long startNanos;
        final long durationNanos;
        final long windupEndNanos;
        final long activeEndNanos;
        final boolean holdLastFrame;
        int lastSentFrame = -1;
        boolean paused = false;
        long pauseStartNanos = 0L;
        long pausedDurationNanos = 0L;

        PlaybackState(Player player,
                      int heldSlot,
                      String sequenceName,
                      int frameCount,
                      long startNanos,
                      long durationNanos,
                      long windupEndNanos,
                      long activeEndNanos,
                      boolean holdLastFrame) {
            this.player = player;
            this.heldSlot = heldSlot;
            this.sequenceName = sequenceName;
            this.frameCount = frameCount;
            this.startNanos = startNanos;
            this.durationNanos = durationNanos;
            this.windupEndNanos = windupEndNanos;
            this.activeEndNanos = activeEndNanos;
            this.holdLastFrame = holdLastFrame;
        }

        double resolveProgress(long elapsed) {
            if (elapsed <= 0L) return 0.0;
            if (elapsed >= durationNanos) return 1.0;
            if (windupEndNanos <= 0L || activeEndNanos <= windupEndNanos || activeEndNanos >= durationNanos) {
                return (double) elapsed / (double) durationNanos;
            }
            if (elapsed <= windupEndNanos) {
                return PHASE_WINDUP_END * ((double) elapsed / (double) windupEndNanos);
            }
            if (elapsed <= activeEndNanos) {
                double t = (double) (elapsed - windupEndNanos) / (double) (activeEndNanos - windupEndNanos);
                return PHASE_WINDUP_END + (PHASE_ACTIVE_END - PHASE_WINDUP_END) * t;
            }
            double t = (double) (elapsed - activeEndNanos) / (double) (durationNanos - activeEndNanos);
            return PHASE_ACTIVE_END + (1.0 - PHASE_ACTIVE_END) * t;
        }
    }
}
