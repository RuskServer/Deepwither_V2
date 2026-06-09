package com.ruskserver.deepwither_V2.core.lifecycle.player;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PlayerLifecycleContext {
    private final Deepwither_V2 plugin;
    private final PlayerLifecycleEventType eventType;
    private final UUID playerId;
    private final String playerName;
    private final Player eventPlayer;

    public PlayerLifecycleContext(Deepwither_V2 plugin, PlayerLifecycleEventType eventType, UUID playerId, String playerName, Player eventPlayer) {
        this.plugin = plugin;
        this.eventType = eventType;
        this.playerId = playerId;
        this.playerName = playerName;
        this.eventPlayer = eventPlayer;
    }

    public Deepwither_V2 plugin() {
        return plugin;
    }

    public PlayerLifecycleEventType eventType() {
        return eventType;
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public Optional<Player> player() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("PlayerLifecycleContext.player() must be called from the main thread");
        }
        if (eventPlayer != null && eventPlayer.isOnline()) {
            return Optional.of(eventPlayer);
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return Optional.empty();
        }
        return Optional.of(player);
    }

    public CompletableFuture<Void> runSync(Runnable runnable) {
        return supplySync(() -> {
            runnable.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supplySync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            completeFuture(future, supplier);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> completeFuture(future, supplier));
        }
        return future;
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return supplyAsync(() -> {
            runnable.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> completeFuture(future, supplier));
        return future;
    }

    private static <T> void completeFuture(CompletableFuture<T> future, Supplier<T> supplier) {
        try {
            future.complete(supplier.get());
        } catch (Exception e) {
            future.completeExceptionally(e);
        } catch (Error e) {
            future.completeExceptionally(e);
            throw e;
        }
    }
}
