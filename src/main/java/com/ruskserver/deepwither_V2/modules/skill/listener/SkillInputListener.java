package com.ruskserver.deepwither_V2.modules.skill.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillCastService;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillSessionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

@Component
public class SkillInputListener implements Listener {

    private final SkillSessionService sessionService;
    private final SkillCastService castService;

    @Inject
    public SkillInputListener(SkillSessionService sessionService, SkillCastService castService) {
        this.sessionService = sessionService;
        this.castService = castService;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) return;

        event.setCancelled(true);
        sessionService.toggleSkillMode(player);
    }

    @EventHandler
    public void onHotbarChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!sessionService.isInSkillMode(player)) return;

        event.setCancelled(true);
        if (castService.isCasting(player)) {
            return;
        }
        sessionService.castSlot(player, event.getNewSlot());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (sessionService.isInSkillMode(event.getPlayer())) {
            sessionService.exitSkillMode(event.getPlayer());
        }
    }
}
