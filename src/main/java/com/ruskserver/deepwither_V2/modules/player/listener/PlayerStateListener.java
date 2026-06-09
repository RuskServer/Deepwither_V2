package com.ruskserver.deepwither_V2.modules.player.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class PlayerStateListener implements Listener, PlayerLifecycleTask {

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.JOIN);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.EARLY;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> context.player().ifPresent(player -> {
            forceAdventure(player);
            setFullFood(player);
        }));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        setFullFood(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            if (event.getPlayer().getGameMode() != GameMode.ADVENTURE) {
                event.getPlayer().setGameMode(GameMode.ADVENTURE);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        event.setCancelled(true);
        setFullFood((Player) event.getEntity());
    }

    private void forceAdventure(Player player) {
        if (player.getGameMode() == GameMode.SURVIVAL) {
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    private void setFullFood(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }
}
