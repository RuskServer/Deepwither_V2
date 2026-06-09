package com.ruskserver.deepwither_V2.modules.skill.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class SkillCooldownService implements PlayerLifecycleTask {

    private final Map<UUID, Map<String, Long>> cooldownUntilMillis = new HashMap<>();

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.QUIT);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.CLEANUP;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> cooldownUntilMillis.remove(context.playerId()));
    }

    public boolean isOnCooldown(UUID playerId, String skillId) {
        return getRemainingMillis(playerId, skillId) > 0L;
    }

    public Duration getRemaining(UUID playerId, String skillId) {
        return Duration.ofMillis(Math.max(0L, getRemainingMillis(playerId, skillId)));
    }

    public void applyCooldown(UUID playerId, String skillId, Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            clearCooldown(playerId, skillId);
            return;
        }
        cooldownUntilMillis
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(skillId, System.currentTimeMillis() + duration.toMillis());
    }

    public void clearCooldown(UUID playerId, String skillId) {
        Map<String, Long> playerCooldowns = cooldownUntilMillis.get(playerId);
        if (playerCooldowns == null) return;
        playerCooldowns.remove(skillId);
        if (playerCooldowns.isEmpty()) {
            cooldownUntilMillis.remove(playerId);
        }
    }

    private long getRemainingMillis(UUID playerId, String skillId) {
        Map<String, Long> playerCooldowns = cooldownUntilMillis.get(playerId);
        if (playerCooldowns == null) return 0L;

        Long until = playerCooldowns.get(skillId);
        if (until == null) return 0L;

        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            clearCooldown(playerId, skillId);
            return 0L;
        }
        return remaining;
    }
}
