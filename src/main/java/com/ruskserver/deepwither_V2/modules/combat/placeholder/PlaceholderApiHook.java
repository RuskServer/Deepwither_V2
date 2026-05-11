package com.ruskserver.deepwither_V2.modules.combat.placeholder;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;

@Service
public class PlaceholderApiHook implements Startable {

    private final Deepwither_V2 plugin;
    private final ManaManager manaManager;
    private final VirtualHealthManager healthManager;

    @Inject
    public PlaceholderApiHook(Deepwither_V2 plugin, ManaManager manaManager, VirtualHealthManager healthManager) {
        this.plugin = plugin;
        this.manaManager = manaManager;
        this.healthManager = healthManager;
    }

    @Override
    public void start() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        new DeepwitherPlaceholderExpansion(plugin, manaManager, healthManager).register();
        plugin.getLogger().info("Registered PlaceholderAPI expansion: %deepwither_mana_current%, %deepwither_mana_max%, %deepwither_health_current%, %deepwither_health_max%");
    }
}
