package com.ruskserver.deepwither_V2.modules.dialogue.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dialogue.service.DialogueService;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Component
public class NPCDialogueClickListener implements Listener {

    private final DialogueService dialogueService;

    @Inject
    public NPCDialogueClickListener(DialogueService dialogueService) {
        this.dialogueService = dialogueService;
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        String npcName = event.getNPC().getName();

        if (dialogueService.hasDialogue(npcName)) {
            if (player.isSneaking()) return;
            event.setCancelled(true);
            dialogueService.startNpcDialogue(player, npcName);
        }
    }
}
