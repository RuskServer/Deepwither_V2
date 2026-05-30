package com.ruskserver.deepwither_V2.modules.trader.definitions;

import com.ruskserver.deepwither_V2.modules.trader.api.TraderDefinition;
import com.ruskserver.deepwither_V2.modules.trader.api.TraderProduct;

import java.util.Arrays;
import java.util.List;

/**
 * "LunarisAtelier" トレーダーの定義。
 * 入門用の魔導具や月光・深淵系の装備を販売します。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class LunariAtelierTrader implements TraderDefinition {

    @Override
    public String getNpcName() {
        return "LunarisAtelier";
    }

    @Override
    public String getDisplayName() {
        return "§f§lLunarisAtelier";
    }

    @Override
    public List<TraderProduct> getProducts() {
        return Arrays.asList(
                new TraderProduct("blue_crystal_sword", 4200.0, 0),
                new TraderProduct("selene_rod", 3500.0, 0),
                new TraderProduct("selene_flow_rod", 6000.0, 320),
                new TraderProduct("moonveil_hood", 8000.0, 500),
                new TraderProduct("moonveil_tunic", 8000.0, 500),
                new TraderProduct("moonveil_leggings", 8000.0, 500),
                new TraderProduct("moonveil_boots", 8000.0, 500),
                new TraderProduct("crystal_sword", 12000.0, 500),
                new TraderProduct("blue_crystal_mace", 58000.0, 1000),
                new TraderProduct("moonlight_tree_bow", 25000.0, 1000),
                new TraderProduct("luna_crave_staff", 45000.0, 1000),
                new TraderProduct("abyssal_moon_jelly", 65000.0, 1000),
                new TraderProduct("moonlight_reaper_scythe", 162000.0, 1000),
                new TraderProduct("moon_shadow_hood", 132000.0, 1000),
                new TraderProduct("moon_shadow_upper", 162000.0, 1000),
                new TraderProduct("moon_shadow_lower", 132000.0, 1000),
                new TraderProduct("moon_shadow_boots", 12000.0, 1000)
        );
    }
}
