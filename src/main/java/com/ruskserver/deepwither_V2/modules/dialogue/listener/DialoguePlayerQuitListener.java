package com.ruskserver.deepwither_V2.modules.dialogue.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dialogue.service.DialogueService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@Component
public class DialoguePlayerQuitListener implements Listener {

    private final DialogueService dialogueService;

    @Inject
    public DialoguePlayerQuitListener(DialogueService dialogueService) {
        this.dialogueService = dialogueService;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dialogueService.endDialogue(event.getPlayer());
    }
}
