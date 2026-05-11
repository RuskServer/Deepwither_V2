package com.ruskserver.deepwither_V2.modules.combat.placeholder;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class DeepwitherPlaceholderExpansion extends PlaceholderExpansion {

    private final Deepwither_V2 plugin;
    private final ManaManager manaManager;
    private final VirtualHealthManager healthManager;

    public DeepwitherPlaceholderExpansion(Deepwither_V2 plugin, ManaManager manaManager, VirtualHealthManager healthManager) {
        this.plugin = plugin;
        this.manaManager = manaManager;
        this.healthManager = healthManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "deepwither";
    }

    @Override
    public @NotNull String getAuthor() {
        return "RuskServer";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "0";

        return switch (params.toLowerCase(Locale.ROOT)) {
            case "mana_current" -> format(manaManager.getMana(player));
            case "mana_max" -> format(manaManager.getMaxMana(player));
            case "health_current" -> format(healthManager.getHealth(player));
            case "health_max" -> format(healthManager.getMaxHealth(player));
            default -> null;
        };
    }

    private String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
