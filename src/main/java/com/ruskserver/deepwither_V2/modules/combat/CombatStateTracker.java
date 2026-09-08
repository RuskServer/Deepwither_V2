package com.ruskserver.deepwither_V2.modules.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/** 戦闘状態はセッション内だけの一時状態。時刻源を分離して解除境界を検証する。 */
public final class CombatStateTracker {
    public static final long EXIT_DELAY_MILLIS = 8_000;
    private final LongSupplier clock;
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Map<UUID, Set<UUID>> encounters = new HashMap<>();

    public CombatStateTracker(LongSupplier clock) {
        this.clock = clock;
    }

    public synchronized void recordActivity(UUID player) {
        lastActivity.put(player, clock.getAsLong());
    }

    public synchronized boolean isInCombat(UUID player) {
        if (encounters.values().stream().anyMatch(members -> members.contains(player))) return true;
        Long last = lastActivity.get(player);
        if (last == null) return false;
        if (clock.getAsLong() - last < EXIT_DELAY_MILLIS) return true;
        lastActivity.remove(player);
        return false;
    }

    public synchronized void joinEncounter(UUID encounter, UUID player) {
        encounters.computeIfAbsent(encounter, ignored -> new HashSet<>()).add(player);
    }

    public synchronized void leaveEncounter(UUID encounter, UUID player) {
        Set<UUID> members = encounters.get(encounter);
        if (members != null) members.remove(player);
    }

    public synchronized void endEncounter(UUID encounter) {
        encounters.remove(encounter);
    }

    public synchronized void recordSupport(UUID source, UUID target) {
        if (source.equals(target) || !isInCombat(target)) return;
        recordActivity(source);
        for (Set<UUID> members : encounters.values()) {
            if (members.contains(target)) members.add(source);
        }
    }

    public synchronized void clearPlayer(UUID player) {
        lastActivity.remove(player);
        encounters.values().forEach(members -> members.remove(player));
    }

    public synchronized void clear() {
        lastActivity.clear();
        encounters.clear();
    }
}
