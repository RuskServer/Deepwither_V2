package com.ruskserver.deepwither_V2.modules.crafting.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.crafting.api.CraftingRecipe;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CraftingRegistry implements Startable {

    private final JavaPlugin plugin;
    private final DIContainer container;
    private final ItemManager itemManager;
    private final Map<String, CraftingRecipe> recipes = new LinkedHashMap<>();

    @Inject
    public CraftingRegistry(JavaPlugin plugin, DIContainer container, ItemManager itemManager) {
        this.plugin = plugin;
        this.container = container;
        this.itemManager = itemManager;
    }

    @Override
    public void start() {
        recipes.clear();
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof CraftingRecipe recipe) {
                register(recipe);
            }
        }
        plugin.getLogger().info("[CraftingRegistry] Loaded " + recipes.size() + " crafting recipe(s).");
    }

    public CraftingRecipe get(String recipeId) {
        return recipes.get(recipeId);
    }

    public List<CraftingRecipe> getForNpc(String npcId) {
        Collator nameCollator = Collator.getInstance(Locale.JAPANESE);
        nameCollator.setStrength(Collator.PRIMARY);
        return recipes.values().stream()
                .filter(recipe -> recipe.getCraftingNpcId().equalsIgnoreCase(npcId))
                .sorted(Comparator.comparing(this::getResultDisplayName, nameCollator)
                        .thenComparing(CraftingRecipe::getId))
                .toList();
    }

    private String getResultDisplayName(CraftingRecipe recipe) {
        CustomItem resultItem = itemManager.getCustomItem(recipe.getResultItemId());
        return resultItem != null ? resultItem.getDisplayName() : recipe.getResultItemId();
    }

    public boolean isCraftingNpc(String npcId) {
        return recipes.values().stream()
                .anyMatch(recipe -> recipe.getCraftingNpcId().equalsIgnoreCase(npcId));
    }

    public Collection<CraftingRecipe> getAll() {
        return List.copyOf(recipes.values());
    }

    private void register(CraftingRecipe recipe) {
        List<String> invalidItemIds = new ArrayList<>();
        if (itemManager.getCustomItem(recipe.getResultItemId()) == null) {
            invalidItemIds.add(recipe.getResultItemId());
        }
        recipe.getIngredients().keySet().stream()
                .filter(itemId -> itemManager.getCustomItem(itemId) == null)
                .forEach(invalidItemIds::add);
        if (!invalidItemIds.isEmpty()) {
            plugin.getLogger().warning("[CraftingRegistry] Skipped recipe " + recipe.getId()
                    + "; unknown item id(s): " + String.join(", ", invalidItemIds));
            return;
        }
        if (recipes.putIfAbsent(recipe.getId(), recipe) != null) {
            plugin.getLogger().warning("[CraftingRegistry] Duplicate recipe id skipped: " + recipe.getId());
        }
    }
}
