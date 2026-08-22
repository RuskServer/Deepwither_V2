package com.ruskserver.deepwither_V2.modules.quest.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.quest.gui.QuestGUI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Component
public class QuestNPCListener implements Listener {

    private static final String TARGET_NPC_NAME = "村長";

    private final QuestGUI questGui;

    @Inject
    public QuestNPCListener(QuestGUI questGui) {
        this.questGui = questGui;
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        String npcName = event.getNPC().getName();
        if (!TARGET_NPC_NAME.equals(npcName)) return;

        Player player = event.getClicker();
        // スニーク時のみダイレクトにデイリークエストGUIを開く（通常クリックは対話システムへ）
        if (player.isSneaking()) {
            event.setCancelled(true);
            questGui.openQuestGui(player);
        }
    }
}
