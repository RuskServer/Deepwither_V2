package com.ruskserver.deepwither_V2.modules.dialogue.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dialogue.service.DialogueService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

@Component
public class DialogueInputListener implements Listener {

    private final DialogueService dialogueService;

    @Inject
    public DialogueInputListener(DialogueService dialogueService) {
        this.dialogueService = dialogueService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!dialogueService.isAwaitingInput(player)) {
            return;
        }

        event.setCancelled(true);

        int prev = event.getPreviousSlot();
        int next = event.getNewSlot();

        int delta;
        if (prev == 8 && next == 0) {
            delta = 1;
        } else if (prev == 0 && next == 8) {
            delta = -1;
        } else {
            delta = next > prev ? 1 : -1;
        }

        dialogueService.scrollChoice(player, delta);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!dialogueService.isAwaitingInput(player)) {
            return;
        }

        event.setCancelled(true);
        dialogueService.confirmCurrentChoice(player);
    }
}
