package com.ruskserver.deepwither_V2.modules.skill.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SkillCooldownService implements Listener {

    private final Map<UUID, Map<String, Long>> cooldownUntilMillis = new HashMap<>();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownUntilMillis.remove(event.getPlayer().getUniqueId());
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
