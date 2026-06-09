package com.ruskserver.deepwither_V2.modules.skill.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerInputStateService implements Listener, PlayerLifecycleTask {

    private final Map<UUID, PlayerInputSnapshot> snapshots = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        snapshots.put(event.getPlayer().getUniqueId(), PlayerInputSnapshot.from(event.getInput()));
    }

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
        snapshots.remove(context.playerId());
        return CompletableFuture.completedFuture(null);
    }

    public PlayerInputSnapshot getSnapshot(Player player) {
        return snapshots.getOrDefault(player.getUniqueId(), PlayerInputSnapshot.empty());
    }

    public Vector getMovementDirection(Player player, Vector fallback) {
        PlayerInputSnapshot snapshot = getSnapshot(player);
        if (!snapshot.hasMovementInput()) {
            return normalizeHorizontal(fallback);
        }

        Vector forward = getHorizontalLookDirection(player.getLocation());
        Vector right = new Vector(-forward.getZ(), 0.0, forward.getX()).normalize();
        Vector direction = new Vector();

        if (snapshot.forward()) {
            direction.add(forward);
        }
        if (snapshot.backward()) {
            direction.subtract(forward);
        }
        if (snapshot.right()) {
            direction.add(right);
        }
        if (snapshot.left()) {
            direction.subtract(right);
        }

        if (direction.lengthSquared() < 0.0001) {
            return normalizeHorizontal(fallback);
        }
        return direction.normalize();
    }

    public Vector getHorizontalLookDirection(Location location) {
        return normalizeHorizontal(location.getDirection());
    }

    private Vector normalizeHorizontal(Vector vector) {
        Vector horizontal = vector == null ? new Vector() : vector.clone();
        horizontal.setY(0.0);
        if (horizontal.lengthSquared() < 0.0001) {
            return new Vector(0.0, 0.0, 1.0);
        }
        return horizontal.normalize();
    }
}
