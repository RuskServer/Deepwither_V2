package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.service.MenuCompassService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@Component
public class MenuCompassListener implements Listener {

    private final MenuCompassService menuCompassService;

    @Inject
    public MenuCompassListener(MenuCompassService menuCompassService) {
        this.menuCompassService = menuCompassService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        menuCompassService.ensureMenuCompass(player);
    }
}
