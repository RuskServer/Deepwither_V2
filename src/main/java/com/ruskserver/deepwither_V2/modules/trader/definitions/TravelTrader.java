package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Collections;
import java.util.List;

/**
 * "旅商人" トレーダーの定義。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class TravelTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "旅商人";
    }

    @Override
    public String getDisplayName() {
        return "§d§l旅商人";
    }

    @Override
    public List<TraderProduct> getProducts() {
        // 現在、Java定義に対応するカスタムアイテムが存在しないため空のリストを返します
        return Collections.emptyList();
    }
}
