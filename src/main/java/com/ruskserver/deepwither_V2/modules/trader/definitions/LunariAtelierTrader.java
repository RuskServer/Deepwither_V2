package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Arrays;
import java.util.List;

/**
 * "Lunaris Atelier" トレーダーの定義。
 * 入門用の魔導具を販売します。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class LunariAtelierTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "Lunaris Atelier";
    }

    @Override
    public String getDisplayName() {
        return "§f§lLunaris Atelier";
    }

    @Override
    public List<TraderProduct> getProducts() {
        return Arrays.asList(
                // Selene Rod - 初級魔導杖
                // 購入: 500ゴールド
                new TraderProduct("selene_rod", 500.0),

                // Selene Flow Rod - 改良魔導杖
                // 購入: 1200ゴールド, 信用度: 320
                new TraderProduct("selene_flow_rod", 1200.0, 320),

                // Blue Crystal Sword - 藍晶の剣
                // 購入: 800ゴールド
                new TraderProduct("blue_crystal_sword", 800.0)
        );
    }
}

