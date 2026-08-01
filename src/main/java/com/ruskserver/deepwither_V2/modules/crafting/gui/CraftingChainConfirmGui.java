package com.ruskserver.deepwither_V2.modules.crafting.gui;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.crafting.api.CraftingRecipe;
import com.ruskserver.deepwither_V2.modules.crafting.service.CraftingChainPlanner;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CraftingChainConfirmGui implements GuiView {

    public static final String ID = "crafting_chain_confirm";
    private static final int MATERIAL_SLOT_LIMIT = 36;

    private final CraftingRegistry registry;
    private final CraftingService craftingService;
    private final ItemManager itemManager;

    @Inject
    public CraftingChainConfirmGui(
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
        return Component.text("中間素材込み一括製作", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public int getSize(Player player, GuiContext context) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        Inventory inventory = context.inventory();
        CraftingGuiSupport.fill(inventory);
        String recipeId = context.context().getString(CraftingRecipeDetailGui.RECIPE_KEY);
        String npcId = context.context().getString(CraftingRecipeListGui.NPC_KEY);
        CraftingRecipe recipe = registry.get(recipeId);
        CraftingChainPlanner.CraftingChainPlan plan = craftingService.getChainPlan(context.player(), recipeId, npcId);

        if (recipe == null) {
            inventory.setItem(22, CraftingGuiSupport.button(Material.BARRIER,
                    Component.text("レシピが見つかりません。", NamedTextColor.RED)));
            inventory.setItem(45, backButton());
            return;
        }

        List<String> materialIds = materialIds(plan);
        for (int index = 0; index < Math.min(materialIds.size(), MATERIAL_SLOT_LIMIT); index++) {
            String itemId = materialIds.get(index);
            inventory.setItem(index, createMaterialIcon(itemId, plan));
        }
        inventory.setItem(40, createSummaryIcon(recipe, plan));
        inventory.setItem(45, backButton());
        inventory.setItem(53, createConfirmButton(context.player(), plan));
    }

    @Override
    public void onClick(GuiClickContext context) {
        if (context.slot() == 45) {
            context.back();
            return;
        }
        if (context.slot() != 53) return;

        String recipeId = context.context().getString(CraftingRecipeDetailGui.RECIPE_KEY);
        String npcId = context.context().getString(CraftingRecipeListGui.NPC_KEY);
        CraftingService.ChainStartResult result = craftingService.startChainCrafting(
                context.player(), recipeId, npcId);
        if (result == CraftingService.ChainStartResult.SUCCESS) {
            context.player().playSound(context.player().getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 0.9f);
            context.player().sendMessage(Component.text("中間素材込みの一括製作を開始しました。", NamedTextColor.GREEN));
            context.open(CraftingRecipeListGui.ID, CraftingRecipeListGui.listContext(
                    npcId, context.context().getInt(CraftingRecipeListGui.PAGE_KEY, 0)));
            return;
        }
        context.player().playSound(context.player().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
        context.player().sendMessage(Component.text(failureMessage(result), NamedTextColor.RED));
        context.rerender();
    }

    private List<String> materialIds(CraftingChainPlanner.CraftingChainPlan plan) {
        Set<String> ids = new LinkedHashSet<>();
        ids.addAll(plan.inventoryConsumption().keySet());
        ids.addAll(plan.missingMaterials().keySet());
        return ids.stream().sorted(Comparator.comparing(this::displayName)).toList();
    }

    private ItemStack createMaterialIcon(String itemId, CraftingChainPlanner.CraftingChainPlan plan) {
        int consumed = plan.inventoryConsumption().getOrDefault(itemId, 0);
        int missing = plan.missingMaterials().getOrDefault(itemId, 0);
        int required = consumed + missing;
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("必要数: " + required, NamedTextColor.GRAY));
        lore.add(Component.text("所持品から消費: " + consumed,
                consumed > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        if (missing > 0) {
            lore.add(Component.text("不足: " + missing, NamedTextColor.RED, TextDecoration.BOLD));
        }
        ItemStack item = CraftingGuiSupport.customItem(itemManager, itemId);
        item.setAmount(Math.min(item.getMaxStackSize(), Math.max(1, required)));
        return CraftingGuiSupport.appendLore(item, lore);
    }

    private ItemStack createSummaryIcon(
            CraftingRecipe recipe,
            CraftingChainPlanner.CraftingChainPlan plan) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("中間工程: " + plan.intermediateCraftCount() + "回", NamedTextColor.YELLOW));
        lore.add(Component.text("合計時間: " + CraftingGuiSupport.formatDuration(plan.totalTime()), NamedTextColor.GRAY));
        lore.add(Component.text("合計製作XP: " + plan.totalExperience(), NamedTextColor.AQUA));
        if (!plan.bonusResults().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("完成時に返却される余剰素材:", NamedTextColor.GREEN));
            plan.bonusResults().forEach((itemId, amount) -> lore.add(
                    Component.text("- " + displayName(itemId) + " x" + amount, NamedTextColor.GRAY)));
        }
        plan.errors().forEach(error -> lore.add(Component.text(error, NamedTextColor.RED)));
        return CraftingGuiSupport.appendLore(
                CraftingGuiSupport.customItem(itemManager, recipe.getResultItemId()), lore);
    }

    private ItemStack createConfirmButton(Player player, CraftingChainPlanner.CraftingChainPlan plan) {
        boolean queueAvailable = craftingService.getJobs(player).size() < CraftingService.MAX_QUEUE_SIZE;
        boolean available = plan.canStart() && queueAvailable;
        List<Component> lore = new ArrayList<>();
        if (!queueAvailable) lore.add(Component.text("製作キューが満杯です。", NamedTextColor.RED));
        if (!plan.missingMaterials().isEmpty()) lore.add(Component.text("原材料が不足しています。", NamedTextColor.RED));
        plan.errors().forEach(error -> lore.add(Component.text(error, NamedTextColor.RED)));
        if (available) {
            lore.add(Component.text("原材料をまとめて消費します。", NamedTextColor.YELLOW));
            lore.add(Component.text("キューは1枠だけ使用します。", NamedTextColor.GRAY));
        }
        return CraftingGuiSupport.button(
                available ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                Component.text(available ? "一括製作を開始" : "一括製作できません",
                        available ? NamedTextColor.GREEN : NamedTextColor.RED,
                        TextDecoration.BOLD),
                lore.toArray(Component[]::new)
        );
    }

    private ItemStack backButton() {
        return CraftingGuiSupport.button(Material.ARROW,
                Component.text("レシピ詳細へ戻る", NamedTextColor.YELLOW));
    }

    private String displayName(String itemId) {
        var item = itemManager.getCustomItem(itemId);
        return item == null ? itemId : item.getDisplayName();
    }

    private String failureMessage(CraftingService.ChainStartResult result) {
        return switch (result) {
            case INVALID_RECIPE -> "このレシピは現在利用できません。";
            case NO_ACTIVE_CHARACTER -> "有効なキャラクターが選択されていません。";
            case QUEUE_FULL -> "製作キューが満杯です。";
            case NO_INTERMEDIATE_STEPS -> "製作可能な中間素材がありません。";
            case MISSING_INGREDIENTS -> "原材料が不足しています。";
            case INVALID_PLAN -> "連鎖製作プランを作成できません。";
            case SUCCESS -> "";
        };
    }
}
