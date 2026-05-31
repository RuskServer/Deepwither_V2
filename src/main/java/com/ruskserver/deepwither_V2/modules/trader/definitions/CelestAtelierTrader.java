package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Arrays;
import java.util.List;

/**
 * "CelestAtelier" トレーダーの定義。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CelestAtelierTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "CelestAtelier";
    }

    @Override
    public String getDisplayName() {
        return "§bCelestAtelier";
    }

    @Override
    public List<TraderProduct> getProducts() {
        return Arrays.asList(
                new TraderProduct("balance_needle", 86000.0, 500),
                new TraderProduct("astral_resonance", 120000.0, 820),
                new TraderProduct("aureole_nova", 180000.0, 1000)
        );
    }
}
