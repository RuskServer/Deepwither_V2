package com.ruskserver.deepwither_V2.modules.dungeon.instance;

import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.DoorConnection;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.DungeonLayout;
import com.ruskserver.deepwither_V2.modules.dungeon.modifier.DungeonModifierContext;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 個別のダンジョンインスタンスの状態を保持します。
 * <p>
 * 生成済みのレイアウト、参加プレイヤー、残りライフ、制限時間に加え、
 * 逐次生成のための未接続ドアキューと深度も管理します。
 */
public class DungeonInstance {

    private final String instanceId;
    private final DungeonDefinition definition;
    private final DungeonLayout layout;
    private final World world;
    private final String worldName;
    private final BlockVector3 origin;
    private final Set<UUID> participants = new HashSet<>();
    private final long startTimeMillis;
    private final int maxLives;

    /** 逐次生成: 次にルームを接続すべき未接続ドア（FIFOキュー） */
    private final List<DoorConnection> pendingDoors = new ArrayList<>();

    private DungeonState state;
    private int remainingLives;
    /** 逐次生成: エントリールーム以外に配置したルーム数（maxDepthとの比較に使用） */
    private int currentDepth;

    /** 疑似乱数生成器（シード値で再現可能） */
    private final Random rng;
    /** 現在の連続通路数（部屋配置時のペース制御に使用） */
    private int consecutiveCorridors;

    private final DungeonModifierContext modifierContext;
    private final long effectiveTimeLimitMillis;
    /** ボス撃破後に有効化される脱出ポータルのワールド座標 */
    private BlockVector3 exitPortalPosition;

    public DungeonInstance(
            String instanceId,
            DungeonDefinition definition,
            DungeonLayout layout,
            World world,
            String worldName,
            BlockVector3 origin,
            int maxLives,
            List<DoorConnection> initialPendingDoors,
            DungeonModifierContext modifierContext
    ) {
        this.instanceId = instanceId;
        this.definition = definition;
        this.layout = layout;
        this.world = world;
        this.worldName = worldName;
        this.origin = origin;
        this.maxLives = maxLives;
        this.remainingLives = maxLives;
        this.startTimeMillis = System.currentTimeMillis();
        this.state = DungeonState.ACTIVE;
        this.pendingDoors.addAll(initialPendingDoors);
        this.currentDepth = 0;
        this.rng = definition.seed() != 0L
                ? new Random(definition.seed() ^ instanceId.hashCode())
                : new Random();
        this.consecutiveCorridors = 0;
        this.modifierContext = modifierContext != null ? modifierContext : DungeonModifierContext.none();
        this.effectiveTimeLimitMillis = (long) (definition.timeLimitMinutes() * 60000L
                * this.modifierContext.combinedTimeLimit());
    }

    // --- 逐次生成 ---

    /**
     * 未接続ドアが残っているかを返します。
     */
    public boolean hasPendingDoors() {
        return !pendingDoors.isEmpty();
    }

    /**
     * 未接続ドアのリストを返します（読み取り専用）。
     */
    public List<DoorConnection> getPendingDoors() {
        return Collections.unmodifiableList(pendingDoors);
    }

    /**
     * 最も古い未接続ドアを1つ取り出します。無い場合は null。
     */
    public DoorConnection pollPendingDoor() {
        return pendingDoors.isEmpty() ? null : pendingDoors.removeFirst();
    }

    /**
     * 未接続ドアを追加します。
     */
    public void addPendingDoors(List<DoorConnection> doors) {
        pendingDoors.addAll(doors);
    }

    /**
     * 指定された未接続ドアをリストから削除します。
     *
     * @return 削除できた場合は true
     */
    public boolean removePendingDoor(DoorConnection door) {
        return pendingDoors.remove(door);
    }

    /**
     * 残っている未接続ドアをすべて取り出します。
     * ボス部屋が確定した際、別分岐から追加のボス部屋が生成されるのを防ぐために使用します。
     */
    public List<DoorConnection> drainPendingDoors() {
        List<DoorConnection> drained = List.copyOf(pendingDoors);
        pendingDoors.clear();
        return drained;
    }

    /**
     * 深度を1増やします。
     */
    public void incrementDepth() {
        currentDepth++;
    }

    /**
     * 現在の深度を返します。
     */
    public int getCurrentDepth() {
        return currentDepth;
    }

    // --- 疑似乱数 / ペース管理 ---

    /**
     * このダンジョンインスタンス専用の乱数生成器を返します。
     */
    public Random getRng() {
        return rng;
    }

    /**
     * 現在の連続通路数を返します。
     */
    public int getConsecutiveCorridors() {
        return consecutiveCorridors;
    }

    /**
     * 連続通路カウンタを1増やします（通路配置時に呼ぶ）。
     */
    public void incrementConsecutiveCorridors() {
        consecutiveCorridors++;
    }

    /**
     * 連続通路カウンタをリセットします（部屋配置時に呼ぶ）。
     */
    public void resetConsecutiveCorridors() {
        consecutiveCorridors = 0;
    }

    // --- 状態管理 ---

    /**
     * ライフを1減らし、0以下なら全滅にします。
     *
     * @return 残りライフ
     */
    public int consumeLife() {
        if (state != DungeonState.ACTIVE) {
            return remainingLives;
        }
        remainingLives--;
        if (remainingLives <= 0) {
            state = DungeonState.WIPED;
        }
        return remainingLives;
    }

    /**
     * ダンジョンをクリア状態にします。
     */
    public void clear() {
        state = DungeonState.CLEARED;
    }

    /**
     * ダンジョンをクリア状態にし、ボス部屋内の脱出ポータルを有効化します。
     */
    public void clear(BlockVector3 exitPortalPosition) {
        if (exitPortalPosition == null) {
            throw new IllegalArgumentException("exitPortalPosition は null にできません");
        }
        this.exitPortalPosition = exitPortalPosition;
        state = DungeonState.CLEARED;
    }

    /**
     * タイムアウト処理を行います。
     */
    public void timeout() {
        state = DungeonState.TIMED_OUT;
    }

    /**
     * ダンジョンをキャンセルします。
     */
    public void cancel() {
        state = DungeonState.CANCELLED;
    }

    /**
     * 制限時間に達したかを判定します。
     */
    public boolean isTimedOut() {
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        return elapsed >= effectiveTimeLimitMillis;
    }

    public long getRemainingTimeMillis() {
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        return Math.max(0, effectiveTimeLimitMillis - elapsed);
    }

    // --- プレイヤー管理 ---

    /**
     * プレイヤーを参加者に追加します。
     */
    public void addParticipant(UUID playerId) {
        participants.add(playerId);
    }

    /**
     * プレイヤーを参加者から削除します。
     */
    public void removeParticipant(UUID playerId) {
        participants.remove(playerId);
    }

    /**
     * 指定プレイヤーが参加者かを判定します。
     */
    public boolean isParticipant(UUID playerId) {
        return participants.contains(playerId);
    }

    // --- Getters ---

    public String getInstanceId() { return instanceId; }
    public DungeonDefinition getDefinition() { return definition; }
    public DungeonLayout getLayout() { return layout; }
    public World getWorld() { return world; }
    public String getWorldName() { return worldName; }
    public BlockVector3 getOrigin() { return origin; }
    public DungeonState getState() { return state; }
    public int getRemainingLives() { return remainingLives; }
    public int getMaxLives() { return maxLives; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public Set<UUID> getParticipants() { return Collections.unmodifiableSet(participants); }
    public DungeonModifierContext getModifierContext() { return modifierContext; }
    public long getEffectiveTimeLimitMillis() { return effectiveTimeLimitMillis; }
    public BlockVector3 getExitPortalPosition() { return exitPortalPosition; }

    /**
     * ダンジョンが進行中か（ACTIVE のみ）を判定します。
     */
    public boolean isActive() {
        return state == DungeonState.ACTIVE;
    }
}
