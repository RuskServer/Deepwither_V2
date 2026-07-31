package com.ruskserver.deepwither_V2.modules.crafting.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.crafting.gui.CraftingRecipeListGui;
import com.ruskserver.deepwither_V2.modules.crafting.service.CraftingRegistry;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiService;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Component
public class CraftingNpcListener implements Listener {

    private final CraftingRegistry registry;
    private final GuiService guiService;

    @Inject
    public CraftingNpcListener(CraftingRegistry registry, GuiService guiService) {
        this.registry = registry;
        this.guiService = guiService;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        String npcId = event.getNPC().getName();
        if (!registry.isCraftingNpc(npcId)) {
            return;
        }
        event.setCancelled(true);
        guiService.open(event.getClicker(), CraftingRecipeListGui.ID, GuiContext.builder()
                .put(CraftingRecipeListGui.NPC_KEY, npcId)
                .put(CraftingRecipeListGui.PAGE_KEY, 0)
                .build());
    }
}
