package com.ruskserver.deepwither_V2.modules.combat.placeholder;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;

@Service
public class PlaceholderApiHook implements Startable {

    private final Deepwither_V2 plugin;
    private final ManaManager manaManager;
    private final VirtualHealthManager healthManager;
    private final PlayerManager playerManager;

    @Inject
    public PlaceholderApiHook(Deepwither_V2 plugin, ManaManager manaManager, VirtualHealthManager healthManager, PlayerManager playerManager) {
        this.plugin = plugin;
        this.manaManager = manaManager;
        this.healthManager = healthManager;
        this.playerManager = playerManager;
    }

    @Override
    public void start() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        new DeepwitherPlaceholderExpansion(plugin, manaManager, healthManager, playerManager).register();
        plugin.getLogger().info("Registered PlaceholderAPI expansion: %deepwither_mana_current%, %deepwither_mana_max%, %deepwither_health_current%, %deepwither_health_max%, %deepwither_level%");
    }
}
