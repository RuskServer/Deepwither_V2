package com.ruskserver.deepwither_V2.modules.crafting.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.crafting.api.CraftingRecipe;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionService;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CraftingChainPlanner {

    private static final int MAX_INTERMEDIATE_CRAFTS = 10_000;

    private final CraftingRegistry registry;
    private final ProfessionService professionService;
    private final ItemPDCUtil itemPDCUtil;

    @Inject
    public CraftingChainPlanner(
            CraftingRegistry registry,
            ProfessionService professionService,
            ItemPDCUtil itemPDCUtil) {
        this.registry = registry;
        this.professionService = professionService;
        this.itemPDCUtil = itemPDCUtil;
    }

    public CraftingChainPlan plan(Player player, CraftingRecipe targetRecipe, String npcId) {
        if (targetRecipe == null || npcId == null || npcId.isBlank()) {
            return CraftingChainPlan.invalid("レシピまたは合成屋が見つかりません。");
        }

        State state = new State(countInventory(player));
        int craftingLevel = professionService.getProgress(player, ProfessionType.CRAFTING).level();
        resolveRecipe(state, targetRecipe, 1, npcId, craftingLevel, false);
        return state.toPlan();
    }

    private void resolveRecipe(
            State state,
            CraftingRecipe recipe,
            int craftCount,
            String npcId,
            int craftingLevel,
            boolean intermediate) {
        if (!recipe.getCraftingNpcId().equalsIgnoreCase(npcId)) {
            state.errors.add("別の合成屋が必要な工程があります: " + recipe.getId());
            return;
        }
        if (craftingLevel < recipe.getRequiredCraftingLevel()) {
            state.errors.add("製作Lv." + recipe.getRequiredCraftingLevel() + "が必要です: " + recipe.getId());
        }
        if (!state.visitingRecipeIds.add(recipe.getId())) {
            state.errors.add("レシピが循環しています: " + recipe.getId());
            return;
        }

        try {
            for (Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
                long required = (long) ingredient.getValue() * craftCount;
                if (required > Integer.MAX_VALUE) {
                    state.errors.add("必要素材数が上限を超えています: " + ingredient.getKey());
                    continue;
                }
                satisfyIngredient(state, ingredient.getKey(), (int) required, npcId, craftingLevel);
            }
            state.addWork(recipe, craftCount);
            if (intermediate) {
                state.intermediateCraftCount += craftCount;
                if (state.intermediateCraftCount > MAX_INTERMEDIATE_CRAFTS) {
                    state.errors.add("中間素材の製作数が上限を超えています。");
                }
            }
        } finally {
            state.visitingRecipeIds.remove(recipe.getId());
        }
    }

    private void satisfyIngredient(
            State state,
            String itemId,
            int required,
            String npcId,
            int craftingLevel) {
        int remaining = required;
        remaining -= take(state.generatedStock, itemId, remaining, null);
        remaining -= take(state.inventoryStock, itemId, remaining, state.inventoryConsumption);
        if (remaining <= 0) return;

        List<CraftingRecipe> producers = registry.getProducingRecipes(itemId, npcId);
        if (producers.isEmpty()) {
            if (!registry.getProducingRecipes(itemId).isEmpty()) {
                state.errors.add("別の合成屋が必要な中間素材です: " + itemId);
                return;
            }
            state.missingMaterials.merge(itemId, remaining, Integer::sum);
            return;
        }
        if (producers.size() > 1) {
            state.errors.add("生産レシピが複数あります: " + itemId);
            return;
        }

        CraftingRecipe producer = producers.getFirst();
        int resultAmount = Math.max(1, producer.getResultAmount());
        int craftCount = (remaining + resultAmount - 1) / resultAmount;
        resolveRecipe(state, producer, craftCount, npcId, craftingLevel, true);
        int produced = craftCount * resultAmount;
        int surplus = produced - remaining;
        if (surplus > 0) {
            state.generatedStock.merge(itemId, surplus, Integer::sum);
        }
    }

    private int take(Map<String, Integer> stock, String itemId, int required, Map<String, Integer> consumed) {
        int available = stock.getOrDefault(itemId, 0);
        int amount = Math.min(available, required);
        if (amount <= 0) return 0;
        if (available == amount) {
            stock.remove(itemId);
        } else {
            stock.put(itemId, available - amount);
        }
        if (consumed != null) {
            consumed.merge(itemId, amount, Integer::sum);
        }
        return amount;
    }

    private Map<String, Integer> countInventory(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            String itemId = itemPDCUtil.getItemId(item);
            if (itemId != null) {
                counts.merge(itemId, item.getAmount(), Integer::sum);
            }
        }
        return counts;
    }

    public record CraftingChainPlan(
            Map<String, Integer> inventoryConsumption,
            Map<String, Integer> missingMaterials,
            Map<String, Integer> bonusResults,
            Duration totalTime,
            long totalExperience,
            int intermediateCraftCount,
            List<String> errors) {

        public boolean canStart() {
            return errors.isEmpty() && missingMaterials.isEmpty() && intermediateCraftCount > 0;
        }

        public static CraftingChainPlan invalid(String error) {
            return new CraftingChainPlan(Map.of(), Map.of(), Map.of(), Duration.ZERO, 0L, 0, List.of(error));
        }
    }

    private static final class State {
        private final Map<String, Integer> inventoryStock;
        private final Map<String, Integer> generatedStock = new HashMap<>();
        private final Map<String, Integer> inventoryConsumption = new LinkedHashMap<>();
        private final Map<String, Integer> missingMaterials = new LinkedHashMap<>();
        private final Set<String> errors = new LinkedHashSet<>();
        private final Set<String> visitingRecipeIds = new HashSet<>();
        private Duration totalTime = Duration.ZERO;
        private long totalExperience;
        private int intermediateCraftCount;

        private State(Map<String, Integer> inventoryStock) {
            this.inventoryStock = new HashMap<>(inventoryStock);
        }

        private void addWork(CraftingRecipe recipe, int count) {
            try {
                totalTime = totalTime.plus(recipe.getCraftingTime().multipliedBy(count));
                totalExperience = Math.addExact(totalExperience,
                        Math.multiplyExact(recipe.getProfessionExperience(), count));
            } catch (ArithmeticException exception) {
                errors.add("製作時間または経験値が上限を超えています。");
            }
        }

        private CraftingChainPlan toPlan() {
            Map<String, Integer> bonuses = new LinkedHashMap<>();
            generatedStock.forEach((itemId, amount) -> {
                if (amount > 0) bonuses.put(itemId, amount);
            });
            return new CraftingChainPlan(
                    Map.copyOf(inventoryConsumption),
                    Map.copyOf(missingMaterials),
                    Map.copyOf(bonuses),
                    totalTime,
                    totalExperience,
                    intermediateCraftCount,
                    List.copyOf(errors)
            );
        }
    }
}
