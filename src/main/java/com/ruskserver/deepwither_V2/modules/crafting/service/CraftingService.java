package com.ruskserver.deepwither_V2.modules.crafting.service;

import com.ruskserver.deepwither_V2.core.database.character.CharacterData;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.crafting.CraftingData;
import com.ruskserver.deepwither_V2.modules.crafting.CraftingJob;
import com.ruskserver.deepwither_V2.modules.crafting.api.CraftingRecipe;
import com.ruskserver.deepwither_V2.modules.crafting.provider.CharacterCraftingProvider;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionService;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CraftingService {

    public static final int MAX_QUEUE_SIZE = 9;

    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final CraftingRegistry registry;
    private final CraftingChainPlanner chainPlanner;
    private final ProfessionService professionService;
    private final ItemManager itemManager;
    private final ItemPDCUtil itemPDCUtil;

    @Inject
    public CraftingService(
            CharacterDataRepository characterDataRepository,
            CharacterService characterService,
            CraftingRegistry registry,
            CraftingChainPlanner chainPlanner,
            ProfessionService professionService,
            ItemManager itemManager,
            ItemPDCUtil itemPDCUtil) {
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.registry = registry;
        this.chainPlanner = chainPlanner;
        this.professionService = professionService;
        this.itemManager = itemManager;
        this.itemPDCUtil = itemPDCUtil;
    }

    public List<CraftingJob> getJobs(Player player) {
        return getContext(player)
                .map(context -> List.copyOf(context.craftingData().getJobs()))
                .orElseGet(List::of);
    }

    public Map<String, Integer> countIngredients(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            String itemId = itemPDCUtil.getItemId(item);
            if (itemId != null) {
                counts.merge(itemId, item.getAmount(), Integer::sum);
            }
        }
        return counts;
    }

    public CraftingAvailability getAvailability(Player player, CraftingRecipe recipe) {
        int level = professionService.getProgress(player, ProfessionType.CRAFTING).level();
        int queueSize = getJobs(player).size();
        Map<String, Integer> owned = countIngredients(player);
        Map<String, Integer> missing = new LinkedHashMap<>();
        recipe.getIngredients().forEach((itemId, required) -> {
            int shortage = required - owned.getOrDefault(itemId, 0);
            if (shortage > 0) {
                missing.put(itemId, shortage);
            }
        });
        return new CraftingAvailability(
                level,
                queueSize,
                level >= recipe.getRequiredCraftingLevel(),
                queueSize < MAX_QUEUE_SIZE,
                missing
        );
    }

    public StartResult startCrafting(Player player, String recipeId) {
        CraftingRecipe recipe = registry.get(recipeId);
        if (recipe == null || itemManager.getCustomItem(recipe.getResultItemId()) == null) {
            return StartResult.INVALID_RECIPE;
        }
        Optional<CraftingContext> contextOptional = getContext(player);
        if (contextOptional.isEmpty()) {
            return StartResult.NO_ACTIVE_CHARACTER;
        }
        CraftingAvailability availability = getAvailability(player, recipe);
        if (!availability.levelMet()) {
            return StartResult.LEVEL_TOO_LOW;
        }
        if (!availability.queueAvailable()) {
            return StartResult.QUEUE_FULL;
        }
        if (!availability.missingIngredients().isEmpty()) {
            return StartResult.MISSING_INGREDIENTS;
        }

        consumeIngredients(player, recipe.getIngredients());
        CraftingContext context = contextOptional.get();
        long completionTime = System.currentTimeMillis() + recipe.getCraftingTime().toMillis();
        context.craftingData().addJob(new CraftingJob(
                UUID.randomUUID(),
                recipe.getId(),
                recipe.getResultItemId(),
                recipe.getResultAmount(),
                completionTime,
                recipe.getProfessionExperience()
        ));
        save(context);
        return StartResult.SUCCESS;
    }

    public CraftingChainPlanner.CraftingChainPlan getChainPlan(Player player, String recipeId, String npcId) {
        CraftingRecipe recipe = registry.get(recipeId);
        if (recipe == null || itemManager.getCustomItem(recipe.getResultItemId()) == null) {
            return CraftingChainPlanner.CraftingChainPlan.invalid("このレシピは現在利用できません。");
        }
        return chainPlanner.plan(player, recipe, npcId);
    }

    public ChainStartResult startChainCrafting(Player player, String recipeId, String npcId) {
        CraftingRecipe recipe = registry.get(recipeId);
        if (recipe == null || itemManager.getCustomItem(recipe.getResultItemId()) == null) {
            return ChainStartResult.INVALID_RECIPE;
        }
        Optional<CraftingContext> contextOptional = getContext(player);
        if (contextOptional.isEmpty()) {
            return ChainStartResult.NO_ACTIVE_CHARACTER;
        }
        if (getJobs(player).size() >= MAX_QUEUE_SIZE) {
            return ChainStartResult.QUEUE_FULL;
        }

        CraftingChainPlanner.CraftingChainPlan plan = chainPlanner.plan(player, recipe, npcId);
        if (plan.intermediateCraftCount() <= 0) {
            return ChainStartResult.NO_INTERMEDIATE_STEPS;
        }
        if (!plan.errors().isEmpty()) {
            return ChainStartResult.INVALID_PLAN;
        }
        if (!plan.missingMaterials().isEmpty()) {
            return ChainStartResult.MISSING_INGREDIENTS;
        }

        long completionTime;
        try {
            completionTime = Math.addExact(System.currentTimeMillis(), plan.totalTime().toMillis());
        } catch (ArithmeticException exception) {
            return ChainStartResult.INVALID_PLAN;
        }
        consumeIngredients(player, plan.inventoryConsumption());
        CraftingContext context = contextOptional.get();
        context.craftingData().addJob(new CraftingJob(
                UUID.randomUUID(),
                recipe.getId(),
                recipe.getResultItemId(),
                recipe.getResultAmount(),
                completionTime,
                plan.totalExperience(),
                plan.bonusResults()
        ));
        save(context);
        return ChainStartResult.SUCCESS;
    }

    public ClaimResult claim(Player player, UUID jobId) {
        Optional<CraftingContext> contextOptional = getContext(player);
        if (contextOptional.isEmpty()) {
            return ClaimResult.NO_ACTIVE_CHARACTER;
        }
        CraftingContext context = contextOptional.get();
        CraftingJob job = context.craftingData().getJobs().stream()
                .filter(candidate -> jobId.equals(candidate.getJobId()))
                .findFirst()
                .orElse(null);
        if (job == null) {
            return ClaimResult.NOT_FOUND;
        }
        if (!job.isFinished()) {
            return ClaimResult.NOT_FINISHED;
        }

        List<ItemStack> results = createResultStacks(job);
        if (results.isEmpty()) {
            return ClaimResult.INVALID_RESULT;
        }
        if (!canFit(player, results)) {
            return ClaimResult.INVENTORY_FULL;
        }

        context.craftingData().removeJob(jobId);
        save(context);
        for (ItemStack result : results) {
            player.getInventory().addItem(result).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        professionService.addExperience(player, ProfessionType.CRAFTING, job.getProfessionExperience());
        return ClaimResult.SUCCESS;
    }

    public ClaimAllResult claimAllFinished(Player player) {
        Optional<CraftingContext> contextOptional = getContext(player);
        if (contextOptional.isEmpty()) {
            return new ClaimAllResult(0, 0, 0, true);
        }

        CraftingContext context = contextOptional.get();
        List<CraftingJob> claimedJobs = new ArrayList<>();
        List<ItemStack> claimedItems = new ArrayList<>();
        int inventoryBlocked = 0;
        int invalidResults = 0;

        for (CraftingJob job : List.copyOf(context.craftingData().getJobs())) {
            if (!job.isFinished()) continue;
            List<ItemStack> results = createResultStacks(job);
            if (results.isEmpty()) {
                invalidResults++;
                continue;
            }
            List<ItemStack> combined = new ArrayList<>(claimedItems);
            combined.addAll(results);
            if (!canFit(player, combined)) {
                inventoryBlocked++;
                continue;
            }
            claimedJobs.add(job);
            claimedItems.addAll(results);
        }

        if (!claimedJobs.isEmpty()) {
            long totalExperience = 0L;
            for (CraftingJob job : claimedJobs) {
                context.craftingData().removeJob(job.getJobId());
                totalExperience += job.getProfessionExperience();
            }
            save(context);
            for (ItemStack result : claimedItems) {
                player.getInventory().addItem(result).values()
                        .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
            professionService.addExperience(player, ProfessionType.CRAFTING, totalExperience);
        }
        return new ClaimAllResult(claimedJobs.size(), inventoryBlocked, invalidResults, false);
    }

    private Optional<CraftingContext> getContext(Player player) {
        return characterService.getActiveCharacter(player.getUniqueId())
                .flatMap(character -> characterDataRepository.get(character.characterId()))
                .map(data -> {
                    CraftingData craftingData = data.get(CharacterCraftingProvider.KEY);
                    if (craftingData == null) {
                        craftingData = new CraftingData();
                        data.set(CharacterCraftingProvider.KEY, craftingData);
                    }
                    return new CraftingContext(data, craftingData);
                });
    }

    private void save(CraftingContext context) {
        context.characterData().markDirty(CharacterCraftingProvider.KEY);
        characterDataRepository.save(context.characterData().getCharacterId(), context.characterData());
    }

    private void consumeIngredients(Player player, Map<String, Integer> ingredients) {
        Map<String, Integer> remaining = new HashMap<>(ingredients);
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length && !remaining.isEmpty(); slot++) {
            ItemStack item = contents[slot];
            String itemId = itemPDCUtil.getItemId(item);
            Integer required = itemId == null ? null : remaining.get(itemId);
            if (required == null) {
                continue;
            }
            int consumed = Math.min(required, item.getAmount());
            item.setAmount(item.getAmount() - consumed);
            int newRequired = required - consumed;
            if (newRequired <= 0) {
                remaining.remove(itemId);
            } else {
                remaining.put(itemId, newRequired);
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    private List<ItemStack> createResultStacks(CraftingJob job) {
        List<ItemStack> results = new ArrayList<>();
        if (!appendResultStacks(results, job.getResultItemId(), job.getResultAmount())) {
            return List.of();
        }
        for (Map.Entry<String, Integer> additional : job.getAdditionalResults().entrySet()) {
            if (!appendResultStacks(results, additional.getKey(), additional.getValue())) {
                return List.of();
            }
        }
        return results;
    }

    private boolean appendResultStacks(List<ItemStack> results, String itemId, int amount) {
        CustomItem definition = itemManager.getCustomItem(itemId);
        if (definition == null || amount <= 0) return false;
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = itemManager.generate(itemId);
            if (stack == null || stack.getType().isAir()) {
                return false;
            }
            int stackAmount = Math.min(stack.getMaxStackSize(), remaining);
            stack.setAmount(stackAmount);
            results.add(stack);
            remaining -= stackAmount;
        }
        return true;
    }

    private boolean canFit(Player player, List<ItemStack> additions) {
        ItemStack[] simulated = Arrays.stream(player.getInventory().getStorageContents())
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);
        for (ItemStack addition : additions) {
            int remaining = addition.getAmount();
            for (ItemStack existing : simulated) {
                if (existing == null || !existing.isSimilar(addition)) {
                    continue;
                }
                int capacity = existing.getMaxStackSize() - existing.getAmount();
                int moved = Math.min(capacity, remaining);
                existing.setAmount(existing.getAmount() + moved);
                remaining -= moved;
                if (remaining <= 0) break;
            }
            for (int slot = 0; slot < simulated.length && remaining > 0; slot++) {
                if (simulated[slot] != null && !simulated[slot].getType().isAir()) {
                    continue;
                }
                ItemStack placed = addition.clone();
                int moved = Math.min(placed.getMaxStackSize(), remaining);
                placed.setAmount(moved);
                simulated[slot] = placed;
                remaining -= moved;
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    public record CraftingAvailability(
            int currentLevel,
            int queueSize,
            boolean levelMet,
            boolean queueAvailable,
            Map<String, Integer> missingIngredients) {

        public boolean canStart() {
            return levelMet && queueAvailable && missingIngredients.isEmpty();
        }
    }

    public enum StartResult {
        SUCCESS,
        INVALID_RECIPE,
        NO_ACTIVE_CHARACTER,
        LEVEL_TOO_LOW,
        QUEUE_FULL,
        MISSING_INGREDIENTS
    }

    public enum ClaimResult {
        SUCCESS,
        NO_ACTIVE_CHARACTER,
        NOT_FOUND,
        NOT_FINISHED,
        INVALID_RESULT,
        INVENTORY_FULL
    }

    public enum ChainStartResult {
        SUCCESS,
        INVALID_RECIPE,
        NO_ACTIVE_CHARACTER,
        QUEUE_FULL,
        NO_INTERMEDIATE_STEPS,
        MISSING_INGREDIENTS,
        INVALID_PLAN
    }

    public record ClaimAllResult(
            int claimedCount,
            int inventoryBlockedCount,
            int invalidResultCount,
            boolean noActiveCharacter) {
    }

    private record CraftingContext(CharacterData characterData, CraftingData craftingData) {
    }
}
