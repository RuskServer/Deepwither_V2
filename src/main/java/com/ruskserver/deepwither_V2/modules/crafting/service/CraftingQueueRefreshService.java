package com.ruskserver.deepwither_V2.modules.crafting.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.crafting.gui.CraftingQueueGui;
import com.ruskserver.deepwither_V2.modules.gui.GuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

@Service
public class CraftingQueueRefreshService implements Startable, Stoppable {

    private static final long REFRESH_INTERVAL_TICKS = 20L;

    private final JavaPlugin plugin;
    private final GuiService guiService;
    private BukkitTask refreshTask;

    @Inject
    public CraftingQueueRefreshService(JavaPlugin plugin, GuiService guiService) {
        this.plugin = plugin;
        this.guiService = guiService;
    }

    @Override
    public void start() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshOpenQueues,
                REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    @Override
    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    private void refreshOpenQueues() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            guiService.rerenderIfOpen(player, CraftingQueueGui.ID);
        }
    }
}
