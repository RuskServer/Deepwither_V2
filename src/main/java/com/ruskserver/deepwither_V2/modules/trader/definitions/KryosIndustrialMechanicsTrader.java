package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Arrays;
import java.util.List;

/**
 * "KryosIndustrialMechanics" トレーダーの定義。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class KryosIndustrialMechanicsTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "KryosIndustrialMechanics";
    }

    @Override
    public String getDisplayName() {
        return "§3KryosIndustrialMechanics";
    }

    @Override
    public List<TraderProduct> getProducts() {
        return Arrays.asList(
                new TraderProduct("gravemind_machete", 42000.0, 0),
                new TraderProduct("boreal_frame_helmet", 16000.0, 0),
                new TraderProduct("boreal_frame_chestplate", 18000.0, 0),
                new TraderProduct("boreal_frame_leggings", 9200.0, 0),
                new TraderProduct("boreal_frame_boots", 8900.0, 0),
                new TraderProduct("glacial_fortress_helmet", 62000.0, 1000),
                new TraderProduct("glacial_fortress_chestplate", 82000.0, 1000),
                new TraderProduct("glacial_fortress_leggings", 82000.0, 1000),
                new TraderProduct("glacial_fortress_boots", 62000.0, 1000),
                new TraderProduct("gravemelt_breaker", 120000.0, 1500),
                new TraderProduct("frostward_testament", 150000.0, 2000)
        );
    }
}
