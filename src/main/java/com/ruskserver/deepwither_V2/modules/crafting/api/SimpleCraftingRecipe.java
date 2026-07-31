package com.ruskserver.deepwither_V2.modules.crafting.api;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class SimpleCraftingRecipe implements CraftingRecipe {

    private final String id;
    private final String resultItemId;
    private final int resultAmount;
    private final String craftingNpcId;
    private final int requiredCraftingLevel;
    private final Duration craftingTime;
    private final long professionExperience;
    private final Map<String, Integer> ingredients;

    protected SimpleCraftingRecipe(
            String id,
            String resultItemId,
            int resultAmount,
            String craftingNpcId,
            int requiredCraftingLevel,
            Duration craftingTime,
            long professionExperience,
            Map<String, Integer> ingredients) {
        this.id = requireText(id, "id");
        this.resultItemId = requireText(resultItemId, "resultItemId");
        this.resultAmount = Math.max(1, resultAmount);
        this.craftingNpcId = requireText(craftingNpcId, "craftingNpcId");
        this.requiredCraftingLevel = Math.max(1, requiredCraftingLevel);
        this.craftingTime = Objects.requireNonNull(craftingTime, "craftingTime");
        if (craftingTime.isNegative()) {
            throw new IllegalArgumentException("craftingTime must not be negative");
        }
        this.professionExperience = Math.max(0L, professionExperience);
        LinkedHashMap<String, Integer> safeIngredients = new LinkedHashMap<>();
        Objects.requireNonNull(ingredients, "ingredients").forEach((itemId, amount) -> {
            String safeItemId = requireText(itemId, "ingredient itemId");
            if (amount == null || amount <= 0) {
                throw new IllegalArgumentException("ingredient amount must be positive: " + safeItemId);
            }
            safeIngredients.put(safeItemId, amount);
        });
        if (safeIngredients.isEmpty()) {
            throw new IllegalArgumentException("ingredients must not be empty");
        }
        this.ingredients = Map.copyOf(safeIngredients);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Integer> getIngredients() {
        return ingredients;
    }

    @Override
    public String getResultItemId() {
        return resultItemId;
    }

    @Override
    public int getResultAmount() {
        return resultAmount;
    }

    @Override
    public String getCraftingNpcId() {
        return craftingNpcId;
    }

    @Override
    public int getRequiredCraftingLevel() {
        return requiredCraftingLevel;
    }

    @Override
    public Duration getCraftingTime() {
        return craftingTime;
    }

    @Override
    public long getProfessionExperience() {
        return professionExperience;
    }
}
