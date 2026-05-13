package com.ruskserver.deepwither_V2.modules.fusion.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.fusion.gui.FusionGui;
import com.ruskserver.deepwither_V2.modules.fusion.service.FusionManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Citizens NPC の右クリックイベントをハンドルし、合成GUIを開きます。
 */
@Component
public class FusionNPCRightClickListener implements Listener {

    private final FusionManager fusionManager;
    private final FusionGui fusionGui;

    @Inject
    public FusionNPCRightClickListener(FusionManager fusionManager, FusionGui fusionGui) {
        this.fusionManager = fusionManager;
        this.fusionGui = fusionGui;
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        String npcName = event.getNPC().getName();

        // このNPC名が合成屋として登録されているか確認
        if (fusionManager.isFusionNpc(npcName)) {
            event.setCancelled(true); // NPCのデフォルトの動作をキャンセル
            fusionGui.openFusionGui(player, npcName);
        }
    }
}
