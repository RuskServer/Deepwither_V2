package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

@Component
public class CelestialWeaveHoodRecipe extends SimpleCraftingRecipe {

    public CelestialWeaveHoodRecipe() {
        super(
                "celestial_weave_hood",
                "celestial_weave_hood",
                1,
                "合成屋",
                2,
                Duration.ofSeconds(30),
                25,
                Map.of("auric_ingot", 3)
        );
    }
}
