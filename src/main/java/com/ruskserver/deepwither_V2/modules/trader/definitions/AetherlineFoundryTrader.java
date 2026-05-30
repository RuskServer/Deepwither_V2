package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Arrays;
import java.util.List;

/**
 * "AetherlineFoundry" トレーダーの定義。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class AetherlineFoundryTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "AetherlineFoundry";
    }

    @Override
    public String getDisplayName() {
        return "§e§lAetherlineFoundry";
    }

    @Override
    public List<TraderProduct> getProducts() {
        return Arrays.asList(
                new TraderProduct("aether_edge", 1800.0, 250),
                new TraderProduct("bastion_breaker", 32000.0, 250),
                new TraderProduct("af07_flux_helmet", 32000.0, 500),
                new TraderProduct("af07_flux_chestplate", 48000.0, 500),
                new TraderProduct("af07_flux_leggings", 48000.0, 500),
                new TraderProduct("af07_flux_boots", 32000.0, 500),
                new TraderProduct("scattered_moon", 48000.0, 650),
                new TraderProduct("aetherium_bulwark_helmet", 62000.0, 1000),
                new TraderProduct("aetherium_bulwark_chestplate", 82000.0, 1000),
                new TraderProduct("aetherium_bulwark_leggings", 82000.0, 1000),
                new TraderProduct("aetherium_bulwark_boots", 62000.0, 1000)
        );
    }
}
