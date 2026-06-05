package com.ruskserver.deepwither_V2.modules.player.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@Component
public class PlayerStateListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        forceAdventure(player);
        setFullFood(player);
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
