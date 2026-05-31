package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Arrays;
import java.util.List;

/**
 * "灰機連盟" トレーダーの定義。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class AshframeUnionTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "灰機連盟";
    }

    @Override
    public String getDisplayName() {
        return "§8灰機連盟";
    }

    @Override
    public List<TraderProduct> getProducts() {
        return Arrays.asList(
                new TraderProduct("residual_ash_halberd", 16000.0, 0),
                new TraderProduct("rusted_iron_branch", 16000.0, 0),
                new TraderProduct("fieldline_bow_rebuilt", 7200.0, 250),
                new TraderProduct("requiem_burst_staff", 62000.0, 650),
                new TraderProduct("ether_shard_halberd", 82000.0, 1000)
        );
    }
}
