package com.ruskserver.deepwither_V2.core.lifecycle.player;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@Service
public class PlayerLifecyclePipeline {
    private final Deepwither_V2 plugin;
    private final Logger logger;
    private final List<PlayerLifecycleTask> tasks;

    @Inject
    public PlayerLifecyclePipeline(Deepwither_V2 plugin, Logger logger, List<PlayerLifecycleTask> tasks) {
        this.plugin = plugin;
        this.logger = logger;
        this.tasks = tasks.stream()
                .sorted(Comparator
                        .comparingInt((PlayerLifecycleTask task) -> task.phase().order())
                        .thenComparingInt(PlayerLifecycleTask::order)
                        .thenComparing(task -> task.getClass().getName()))
                .toList();
    }

    public void run(PlayerLifecycleEventType eventType, org.bukkit.entity.Player player) {
        PlayerLifecycleContext context = new PlayerLifecycleContext(plugin, eventType, player.getUniqueId(), player.getName(), player);
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (PlayerLifecycleTask task : tasks) {
            if (!task.eventTypes().contains(eventType)) continue;
            chain = chain.thenCompose(ignored -> runTask(task, context));
        }

        chain.exceptionally(error -> {
            LogRecord record = new LogRecord(Level.SEVERE, "Player lifecycle pipeline failed: event={0}, player={1}, name={2}");
            record.setParameters(new Object[]{eventType, player.getUniqueId(), player.getName()});
            record.setThrown(error);
            logger.log(record);
            return null;
        });
    }

    private CompletableFuture<Void> runTask(PlayerLifecycleTask task, PlayerLifecycleContext context) {
        try {
            CompletableFuture<Void> future = task.run(context);
            if (future == null) {
                logger.log(Level.SEVERE, "Player lifecycle task {0} returned null from run(): contract violation",
                        task.getClass().getSimpleName());
                return CompletableFuture.completedFuture(null);
            }
            return future.exceptionally(error -> {
                logger.log(Level.SEVERE, "Player lifecycle task failed: " + task.getClass().getSimpleName(), error);
                return null;
            });
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Player lifecycle task failed: " + task.getClass().getSimpleName(), t);
            return CompletableFuture.completedFuture(null);
        }
    }
}
