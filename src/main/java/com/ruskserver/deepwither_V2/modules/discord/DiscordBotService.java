package com.ruskserver.deepwither_V2.modules.discord;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.discord.listener.DiscordBridge;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service
public class DiscordBotService implements Startable, Stoppable {

    private static final int MAX_RETRIES = 60;
    private static final long RETRY_DELAY_MS = 5_000L;

    private final Deepwither_V2 plugin;
    private final DiscordBridge bridge;
    private final Logger log;

    @Inject
    public DiscordBotService(Deepwither_V2 plugin, DiscordBridge bridge, Logger log) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.log = log;
    }

    @Override
    public void start() {
        log.info("[DiscordBot] Scheduling Discord bridge registration...");
        scheduleRegister(0);
    }

    private void scheduleRegister(int attempt) {
        Bukkit.getAsyncScheduler().runDelayed(plugin, scheduled -> {
            try {
                JDA jda = DiscordSRV.getPlugin().getJda();
                if (jda == null || jda.getStatus() != JDA.Status.CONNECTED) {
                    if (attempt >= MAX_RETRIES) {
                        log.warning("[DiscordBot] JDA never became ready. Discord bot disabled.");
                        return;
                    }
                    scheduleRegister(attempt + 1);
                    return;
                }

                jda.addEventListener(bridge);

                User self = jda.getSelfUser();
                log.info("[DiscordBot] Bridge registered on JDA. Bot=" + self.getName() + " (id=" + self.getId() + ")");

            } catch (Throwable t) {
                log.warning("[DiscordBot] Failed to register: " + t.getMessage());
            }
        }, attempt == 0 ? 50L : RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        log.info("[DiscordBot] Shutting down (DiscordSRV manages JDA lifecycle).");
    }
}
