package com.ruskserver.deepwither_V2.modules.dialogue.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dialogue.service.DialogueService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public class DialogueChatHandler implements Listener {

    private final DialogueService dialogueService;
    private final JavaPlugin plugin;

    @Inject
    public DialogueChatHandler(DialogueService dialogueService, JavaPlugin plugin) {
        this.dialogueService = dialogueService;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!dialogueService.isInDialogue(event.getPlayer())) return;

        String message = event.getMessage().trim();
        int index;

        try {
            index = Integer.parseInt(message) - 1;
        } catch (NumberFormatException e) {
            return;
        }

        event.setCancelled(true);

        final int choiceIndex = index;
        Bukkit.getScheduler().runTask(plugin,
                () -> dialogueService.selectChoice(event.getPlayer(), choiceIndex));
    }
}
