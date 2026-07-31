package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

@Component
public class UsedIronPickaxeRecipe extends SimpleCraftingRecipe {

    public UsedIronPickaxeRecipe() {
        super(
                "used_iron_pickaxe_upgrade",
                "used_iron_pickaxe",
                1,
                "合成屋",
                1,
                Duration.ofSeconds(20),
                20,
                Map.of("lunaris_survey_pickaxe", 1, "auric_ingot", 2)
        );
    }
}
