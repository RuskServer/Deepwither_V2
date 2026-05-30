package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Arrays;
import java.util.List;

/**
 * "LazwardDynamics" トレーダーの定義。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class LazwardDynamicsTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "LazwardDynamics";
    }

    @Override
    public String getDisplayName() {
        return "§9§lLazwardDynamics";
    }

    @Override
    public List<TraderProduct> getProducts() {
        return Arrays.asList(
                new TraderProduct("variant_spear", 24000.0, 0),
                new TraderProduct("fieldline_bow", 2500.0, 0),
                new TraderProduct("laps_visor", 8500.0, 0),
                new TraderProduct("laps_jacket", 7500.0, 0),
                new TraderProduct("laps_stride", 6500.0, 0),
                new TraderProduct("laps_tread", 6500.0, 0),
                new TraderProduct("tactical_dagger", 9200.0, 0),
                new TraderProduct("accel_laps_visor", 28000.0, 500),
                new TraderProduct("accel_laps_jacket", 34000.0, 500),
                new TraderProduct("accel_laps_stride", 34000.0, 500),
                new TraderProduct("accel_laps_tread", 28000.0, 500),
                new TraderProduct("kobalt_reverb_mace", 52000.0, 1000),
                new TraderProduct("hammer_mk1", 75000.0, 1500),
                new TraderProduct("tactical_dagger_td10", 62000.0, 1800)
        );
    }
}
