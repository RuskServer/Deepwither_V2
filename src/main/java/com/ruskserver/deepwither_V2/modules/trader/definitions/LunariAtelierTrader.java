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
                // 購入: 500ゴールド, 売却: 250ゴールド
                new TraderProduct("selene_rod", 500.0, 250.0),

                // Blue Crystal Sword - 藍晶の剣
                // 購入: 800ゴールド, 売却: 400ゴールド
                new TraderProduct("blue_crystal_sword", 800.0, 400.0)
        );
    }
}

