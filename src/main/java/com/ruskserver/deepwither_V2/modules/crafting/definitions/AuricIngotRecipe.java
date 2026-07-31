package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

@Component
public class AuricIngotRecipe extends SimpleCraftingRecipe {

    public AuricIngotRecipe() {
        super(
                "auric_ingot_refining",
                "auric_ingot",
                1,
                "合成屋",
                1,
                Duration.ofSeconds(10),
                15,
                Map.of("raw_gold_chunk", 5)
        );
    }
}
