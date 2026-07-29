package com.ruskserver.deepwither_V2.modules.stash.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.modules.stash.gui.StashGuiService;
import com.ruskserver.deepwither_V2.modules.stash.gui.StashInventoryHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class StashListener implements Listener, PlayerLifecycleTask {

    private final Deepwither_V2 plugin;
    private final StashGuiService stashGuiService;

    @Inject
    public StashListener(Deepwither_V2 plugin, StashGuiService stashGuiService) {
        this.plugin = plugin;
        this.stashGuiService = stashGuiService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderChestInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.ENDER_CHEST) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                stashGuiService.open(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StashInventoryHolder holder) {
            stashGuiService.handleClick(holder, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StashInventoryHolder holder) {
            stashGuiService.handleDrag(holder, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof StashInventoryHolder holder) {
            stashGuiService.handleClose(holder);
        }
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.QUIT);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.CLEANUP;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        stashGuiService.closePlayer(context.playerId());
        return CompletableFuture.completedFuture(null);
    }
}
