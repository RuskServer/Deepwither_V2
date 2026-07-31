package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

@Component
public class CelestialWeaveTunicRecipe extends SimpleCraftingRecipe {

    public CelestialWeaveTunicRecipe() {
        super(
                "celestial_weave_tunic",
                "celestial_weave_tunic",
                1,
                "合成屋",
                5,
                Duration.ofSeconds(75),
                60,
                Map.of("auric_ingot", 8, "cut_aether_diamond", 2)
        );
    }
}
