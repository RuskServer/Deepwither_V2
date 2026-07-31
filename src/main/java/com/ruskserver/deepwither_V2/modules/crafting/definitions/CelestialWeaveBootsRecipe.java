package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

@Component
public class CelestialWeaveBootsRecipe extends SimpleCraftingRecipe {

    public CelestialWeaveBootsRecipe() {
        super(
                "celestial_weave_boots",
                "celestial_weave_boots",
                1,
                "合成屋",
                3,
                Duration.ofSeconds(40),
                30,
                Map.of("auric_ingot", 4)
        );
    }
}
