package com.ruskserver.deepwither_V2.modules.crafting.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.crafting.gui.CraftingRecipeListGui;
import com.ruskserver.deepwither_V2.modules.crafting.service.CraftingRegistry;
import com.ruskserver.deepwither_V2.modules.dialogue.service.DialogueService;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiService;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Component
public class CraftingNpcListener implements Listener {

    private final CraftingRegistry registry;
    private final GuiService guiService;
    private final DialogueService dialogueService;

    @Inject
    public CraftingNpcListener(CraftingRegistry registry, GuiService guiService, DialogueService dialogueService) {
        this.registry = registry;
        this.guiService = guiService;
        this.dialogueService = dialogueService;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        String npcId = event.getNPC().getName();
        if (!registry.isCraftingNpc(npcId)) {
            return;
        }

        Player player = event.getClicker();

        // 会話定義が存在するNPCの場合、通常クリックは会話システムへ委譲し、スニーク時のみ直接GUIを開く
        if (dialogueService.hasDialogue(npcId) && !player.isSneaking()) {
            return;
        }

        event.setCancelled(true);
        guiService.open(player, CraftingRecipeListGui.ID, GuiContext.builder()
                .put(CraftingRecipeListGui.NPC_KEY, npcId)
                .put(CraftingRecipeListGui.PAGE_KEY, 0)
                .build());
    }
}
