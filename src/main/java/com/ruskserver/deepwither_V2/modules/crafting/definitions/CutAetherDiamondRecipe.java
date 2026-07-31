package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

@Component
public class CutAetherDiamondRecipe extends SimpleCraftingRecipe {

    public CutAetherDiamondRecipe() {
        super(
                "cut_aether_diamond_polishing",
                "cut_aether_diamond",
                1,
                "合成屋",
                3,
                Duration.ofSeconds(20),
                25,
                Map.of("rough_diamond", 3)
        );
    }
}
