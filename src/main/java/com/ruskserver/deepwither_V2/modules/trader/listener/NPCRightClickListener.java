package com.ruskserver.deepwither_V2.modules.trader.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.trader.gui.SellInventoryGui;
import com.ruskserver.deepwither_V2.modules.trader.gui.TraderInventoryGui;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Citizens NPC の右クリックイベントおよびインベントリ操作をハンドルします。
 */
@Component
public class NPCRightClickListener implements Listener {

    private final TraderService traderService;
    private final TraderInventoryGui gui;
    private final SellInventoryGui sellGui;

    @Inject
    public NPCRightClickListener(TraderService traderService, TraderInventoryGui gui, SellInventoryGui sellGui) {
        this.traderService = traderService;
        this.gui = gui;
        this.sellGui = sellGui;
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            gui.closeTrader(player);
            sellGui.closeSellGui(player);
        }
    }
}

