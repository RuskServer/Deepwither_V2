package com.ruskserver.deepwither_V2.modules.trader.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.trader.gui.TraderInventoryGui;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Citizens NPC の右クリックイベントをハンドルします。
 * トレーダーNPCが右クリックされた時、GUI を開きます。
 */
@Component
public class NPCRightClickListener implements Listener {

    private final TraderService traderService;
    private final TraderInventoryGui gui;

    @Inject
    public NPCRightClickListener(TraderService traderService, TraderInventoryGui gui) {
        this.traderService = traderService;
        this.gui = gui;
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        String npcName = event.getNPC().getName();

        // このNPC名がトレーダー定義に存在するか確認
        if (traderService.getTrader(npcName) != null) {
            gui.openTraderGui(player, npcName);
        }
    }
}

