package com.ruskserver.deepwither_V2.modules.mining;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

@Component
public class MiningListener implements Listener {

    private final MiningService miningService;

    @Inject
    public MiningListener(MiningService miningService) {
        this.miningService = miningService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!miningService.isTrackedOre(event.getBlock().getType())) {
            return;
        }
        event.setCancelled(true);
        miningService.handleMiningAttempt(event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        miningService.protectFromExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        miningService.protectFromExplosion(event.blockList());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        miningService.handleChunkLoad(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        miningService.handleWorldLoad(event.getWorld());
    }
}
