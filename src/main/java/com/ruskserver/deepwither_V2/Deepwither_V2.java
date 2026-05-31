package com.ruskserver.deepwither_V2;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.logging.Level;

public final class Deepwither_V2 extends JavaPlugin {

    private DIContainer container;

    @Override
    public void onEnable() {
        getLogger().info("Starting Deepwither_V2 Module Monolith...");
        
        container = new DIContainer();
        container.setLogger(getLogger());
        container.setDebugMode(true); // 開発中はtrueにする
        
        // Register the plugin instance itself so it can be injected
        container.registerInstance(JavaPlugin.class, this);
        container.registerInstance(Deepwither_V2.class, this);
        container.registerInstance(java.util.logging.Logger.class, getLogger());

        try {
            // Scan and register all modules
            String basePackage = "com.ruskserver.deepwither_V2";
            container.scanAndRegister(this.getClassLoader(), basePackage);
            
            // Start components
            container.startAll();
            
            // Register Spigot listeners
            registerListeners();
            
            // Register Paper Commands
            registerCommands();
            
            // Print dependency tree to console
            container.printDependencyTree();
            
            getLogger().info("Deepwither_V2 successfully initialized.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize DI Container. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (container != null) {
            container.stopAll();
        }
        getLogger().info("Deepwither_V2 successfully shut down.");
    }

    private void registerListeners() {
        int count = 0;
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof Listener) {
                Bukkit.getPluginManager().registerEvents((Listener) instance, this);
                count++;
            }
        }
        getLogger().info("Registered " + count + " auto-detected Listeners.");
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            int count = 0;
            for (Object instance : container.getAllInstances()) {
                if (instance instanceof BasicCommand && instance.getClass().isAnnotationPresent(Command.class)) {
                    Command cmdInfo = instance.getClass().getAnnotation(Command.class);
                    event.registrar().register(
                            cmdInfo.name(),
                            cmdInfo.description(),
                            Arrays.asList(cmdInfo.aliases()),
                            (BasicCommand) instance
                    );
                    count++;
                }
            }
            getLogger().info("Registered " + count + " auto-detected Commands.");
        });
    }
}
