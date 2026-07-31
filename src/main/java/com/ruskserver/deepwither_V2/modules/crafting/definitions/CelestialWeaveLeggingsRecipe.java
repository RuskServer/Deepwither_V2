package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

@Component
public class CelestialWeaveLeggingsRecipe extends SimpleCraftingRecipe {

    public CelestialWeaveLeggingsRecipe() {
        super(
                "celestial_weave_leggings",
                "celestial_weave_leggings",
                1,
                "合成屋",
                4,
                Duration.ofSeconds(60),
                45,
                Map.of("auric_ingot", 6, "cut_aether_diamond", 1)
        );
    }
}
