package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

abstract class AbstractGhoulSetRecipe extends SimpleCraftingRecipe {
    protected AbstractGhoulSetRecipe(String itemId, int level, int seconds, long experience,
                                     Map<String, Integer> ingredients) {
        super(itemId, itemId, 1, "合成屋", level, Duration.ofSeconds(seconds), experience, ingredients);
    }
}
