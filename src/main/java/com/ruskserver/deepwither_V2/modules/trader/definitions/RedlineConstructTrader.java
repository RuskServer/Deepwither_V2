package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Arrays;
import java.util.List;

/**
 * "RedlineConstruct" トレーダーの定義。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class RedlineConstructTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "RedlineConstruct";
    }

    @Override
    public String getDisplayName() {
        return "§cRedlineConstruct";
    }

    @Override
    public List<TraderProduct> getProducts() {
        return Arrays.asList(
                new TraderProduct("ruby_bow_07", 120000.0, 0),
                new TraderProduct("ruby_sword_03", 42000.0, 0),
                new TraderProduct("ruby_axe_02", 42000.0, 0),
                new TraderProduct("ruby_spear_04", 62000.0, 0),
                new TraderProduct("rc_ruby_staff_prototype", 92000.0, 500)
        );
    }
}
