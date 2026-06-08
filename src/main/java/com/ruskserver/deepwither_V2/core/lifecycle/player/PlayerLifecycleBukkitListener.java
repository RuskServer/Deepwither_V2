package com.ruskserver.deepwither_V2.core.lifecycle.player;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Component
public class PlayerLifecycleBukkitListener implements Listener {
    private final PlayerLifecyclePipeline pipeline;

    @Inject
    public PlayerLifecycleBukkitListener(PlayerLifecyclePipeline pipeline) {
        this.pipeline = pipeline;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        pipeline.run(PlayerLifecycleEventType.JOIN, event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pipeline.run(PlayerLifecycleEventType.QUIT, event.getPlayer());
    }
}
