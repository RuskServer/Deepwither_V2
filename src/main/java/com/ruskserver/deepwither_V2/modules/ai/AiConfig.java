package com.ruskserver.deepwither_V2.modules.ai;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.ai.api.RateLimiter;
import com.ruskserver.deepwither_V2.modules.ai.chat.ChatManager;
import java.io.File;
import java.util.logging.Logger;

@Service
public class AiConfig implements Startable {

    private final Deepwither_V2 plugin;
    private final ChatManager chatManager;
    private final RateLimiter rateLimiter;
    private final Logger log;

    @Inject
    public AiConfig(Deepwither_V2 plugin, ChatManager chatManager,
                    RateLimiter rateLimiter, Logger log) {
        this.plugin = plugin;
        this.chatManager = chatManager;
        this.rateLimiter = rateLimiter;
        this.log = log;
    }

    @Override
    public void start() {
        saveDefaultConfig();
        loadConfig();
    }

    private void saveDefaultConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    private void loadConfig() {
        plugin.reloadConfig();
        var config = plugin.getConfig();

        String apiKey = config.getString("ai.google-api-key", "");
        int dailyLimit = config.getInt("ai.daily-limit", 1500);
        int globalRpm = config.getInt("ai.global-rpm", 15);
        int userRpm = config.getInt("ai.user-rpm", 3);
        int timeout = config.getInt("ai.thinking-timeout-seconds", 300);
        String modelPath = config.getString("ai.model-path", "plugins/Deepwither_V2/models/gemma-300m-onnx");

        System.setProperty("ai.djl.onnxruntime.model_path", modelPath);

        rateLimiter.configure(globalRpm, userRpm, dailyLimit);

        if (!apiKey.isEmpty()) {
            chatManager.initializeApi(apiKey, timeout);
        }

        log.info("[AiConfig] Loaded AI config: dailyLimit=" + dailyLimit
                + ", globalRpm=" + globalRpm + ", userRpm=" + userRpm);
    }
}
