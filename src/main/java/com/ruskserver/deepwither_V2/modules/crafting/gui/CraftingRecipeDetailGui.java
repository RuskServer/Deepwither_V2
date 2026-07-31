package com.ruskserver.deepwither_V2.modules.crafting.gui;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.crafting.api.CraftingRecipe;
import com.ruskserver.deepwither_V2.modules.crafting.service.CraftingRegistry;
import com.ruskserver.deepwither_V2.modules.crafting.service.CraftingService;
import com.ruskserver.deepwither_V2.modules.gui.GuiClickContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiRenderContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiView;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CraftingRecipeDetailGui implements GuiView {

    public static final String ID = "crafting_recipe_detail";
    public static final String RECIPE_KEY = "recipe";
    private static final int[] INGREDIENT_SLOTS = {9, 10, 11, 15, 16, 17};

    private final CraftingRegistry registry;
    private final CraftingService craftingService;
    private final ItemManager itemManager;

    @Inject
    public CraftingRecipeDetailGui(
            CraftingRegistry registry,
            CraftingService craftingService,
            ItemManager itemManager) {
        this.registry = registry;
        this.craftingService = craftingService;
        this.itemManager = itemManager;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getTitle(Player player, GuiContext context) {
        return Component.text("製作レシピ詳細", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public int getSize(Player player, GuiContext context) {
        return 27;
    }

    @Override
    public void render(GuiRenderContext context) {
        Inventory inventory = context.inventory();
        CraftingGuiSupport.fill(inventory);
        CraftingRecipe recipe = registry.get(context.context().getString(RECIPE_KEY));
        if (recipe == null) {
            inventory.setItem(13, CraftingGuiSupport.button(
                    Material.BARRIER,
                    Component.text("レシピが見つかりません", NamedTextColor.RED)
            ));
            inventory.setItem(18, backButton());
            return;
        }

        Map<String, Integer> owned = craftingService.countIngredients(context.player());
        CraftingService.CraftingAvailability availability = craftingService.getAvailability(context.player(), recipe);
        inventory.setItem(13, createResultIcon(recipe, availability));

        int ingredientIndex = 0;
        for (Map.Entry<String, Integer> ingredient : recipe.getIngredients().entrySet()) {
            if (ingredientIndex >= INGREDIENT_SLOTS.length) break;
            inventory.setItem(INGREDIENT_SLOTS[ingredientIndex++], createIngredientIcon(
                    ingredient.getKey(),
                    ingredient.getValue(),
                    owned.getOrDefault(ingredient.getKey(), 0)
            ));
        }

        inventory.setItem(18, backButton());
        inventory.setItem(22, createStartButton(recipe, availability));
        inventory.setItem(26, CraftingGuiSupport.button(
                Material.CHEST,
                Component.text("製作キュー", NamedTextColor.AQUA),
                Component.text(availability.queueSize() + " / " + CraftingService.MAX_QUEUE_SIZE, NamedTextColor.GRAY)
        ));
    }

    @Override
    public void onClick(GuiClickContext context) {
        String npcId = context.context().getString(CraftingRecipeListGui.NPC_KEY);
        int page = context.context().getInt(CraftingRecipeListGui.PAGE_KEY, 0);
        if (context.slot() == 18) {
            context.open(CraftingRecipeListGui.ID, CraftingRecipeListGui.listContext(npcId, page));
            return;
        }
        if (context.slot() == 26) {
            context.open(CraftingQueueGui.ID, GuiContext.builder()
                    .put(CraftingRecipeListGui.NPC_KEY, npcId)
                    .build());
            return;
        }
        if (context.slot() != 22) {
            return;
        }

        CraftingService.StartResult result = craftingService.startCrafting(
                context.player(),
                context.context().getString(RECIPE_KEY)
        );
        if (result == CraftingService.StartResult.SUCCESS) {
            context.player().playSound(context.player().getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.1f);
            context.player().sendMessage(Component.text("製作を開始しました。", NamedTextColor.GREEN));
            context.open(CraftingQueueGui.ID, GuiContext.builder()
                    .put(CraftingRecipeListGui.NPC_KEY, npcId)
                    .build());
            return;
        }
        context.player().playSound(context.player().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
        context.player().sendMessage(Component.text(startFailureMessage(result), NamedTextColor.RED));
        context.refresh();
    }

    private ItemStack createResultIcon(CraftingRecipe recipe, CraftingService.CraftingAvailability availability) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("完成数: x" + recipe.getResultAmount(), NamedTextColor.WHITE));
        lore.add(Component.text("必要製作Lv: ", NamedTextColor.GRAY)
                .append(Component.text(recipe.getRequiredCraftingLevel(),
                        availability.levelMet() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.text("製作時間: " + CraftingGuiSupport.formatDuration(recipe.getCraftingTime()), NamedTextColor.YELLOW));
        lore.add(Component.text("獲得製作EXP: " + recipe.getProfessionExperience(), NamedTextColor.AQUA));
        return CraftingGuiSupport.appendLore(
                CraftingGuiSupport.customItem(itemManager, recipe.getResultItemId()), lore);
    }

    private ItemStack createIngredientIcon(String itemId, int required, int owned) {
        boolean enough = owned >= required;
        ItemStack item = CraftingGuiSupport.customItem(itemManager, itemId);
        item.setAmount(Math.min(item.getMaxStackSize(), Math.max(1, required)));
        return CraftingGuiSupport.appendLore(item, List.of(
                Component.empty(),
                Component.text("必要: " + required, NamedTextColor.GRAY),
                Component.text("所持: " + owned, enough ? NamedTextColor.GREEN : NamedTextColor.RED),
                Component.text(enough ? "素材充足" : "あと " + (required - owned) + " 個必要",
                        enough ? NamedTextColor.GREEN : NamedTextColor.RED)
        ));
    }

    private ItemStack createStartButton(CraftingRecipe recipe, CraftingService.CraftingAvailability availability) {
        List<Component> lore = new ArrayList<>();
        if (!availability.levelMet()) {
            lore.add(Component.text("製作Lvが不足しています", NamedTextColor.RED));
        }
        if (!availability.queueAvailable()) {
            lore.add(Component.text("製作キューが満杯です", NamedTextColor.RED));
        }
        if (!availability.missingIngredients().isEmpty()) {
            lore.add(Component.text("必要素材が不足しています", NamedTextColor.RED));
        }
        if (availability.canStart()) {
            lore.add(Component.text("素材を消費して製作を開始します", NamedTextColor.YELLOW));
            lore.add(Component.text("完成まで " + CraftingGuiSupport.formatDuration(recipe.getCraftingTime()), NamedTextColor.GRAY));
        }
        return CraftingGuiSupport.button(
                availability.canStart() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                Component.text(availability.canStart() ? "製作開始" : "製作不可",
                        availability.canStart() ? NamedTextColor.GREEN : NamedTextColor.RED,
                        TextDecoration.BOLD),
                lore.toArray(Component[]::new)
        );
    }

    private ItemStack backButton() {
        return CraftingGuiSupport.button(
                Material.ARROW,
                Component.text("レシピ一覧へ戻る", NamedTextColor.YELLOW)
        );
    }

    private String startFailureMessage(CraftingService.StartResult result) {
        return switch (result) {
            case LEVEL_TOO_LOW -> "製作レベルが不足しています。";
            case QUEUE_FULL -> "製作キューが満杯です。";
            case MISSING_INGREDIENTS -> "必要素材が不足しています。";
            case NO_ACTIVE_CHARACTER -> "有効なキャラクターが選択されていません。";
            case INVALID_RECIPE -> "このレシピは現在利用できません。";
            case SUCCESS -> "";
        };
    }
}
