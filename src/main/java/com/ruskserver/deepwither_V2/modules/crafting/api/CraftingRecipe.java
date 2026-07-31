package com.ruskserver.deepwither_V2.modules.crafting.api;

import java.time.Duration;
import java.util.Map;

public interface CraftingRecipe {

    String getId();

    Map<String, Integer> getIngredients();

    String getResultItemId();

    default int getResultAmount() {
        return 1;
    }

    String getCraftingNpcId();

    default int getRequiredCraftingLevel() {
        return 1;
    }

    Duration getCraftingTime();

    long getProfessionExperience();
}
