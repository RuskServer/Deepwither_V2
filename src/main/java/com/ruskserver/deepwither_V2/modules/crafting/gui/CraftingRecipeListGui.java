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
import com.ruskserver.deepwither_V2.modules.profession.ProfessionService;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionType;
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

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CraftingRecipeListGui implements GuiView {

    public static final String ID = "crafting_recipes";
    public static final String NPC_KEY = "crafting_npc";
    public static final String PAGE_KEY = "page";
    private static final int RECIPES_PER_PAGE = 45;

    private final CraftingRegistry registry;
    private final CraftingService craftingService;
    private final ProfessionService professionService;
    private final ItemManager itemManager;

    @Inject
    public CraftingRecipeListGui(
            CraftingRegistry registry,
            CraftingService craftingService,
            ProfessionService professionService,
            ItemManager itemManager) {
        this.registry = registry;
        this.craftingService = craftingService;
        this.professionService = professionService;
        this.itemManager = itemManager;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getTitle(Player player, GuiContext context) {
        return Component.text(resolveNpc(context) + " - 製作レシピ", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public int getSize(Player player, GuiContext context) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        Player player = context.player();
        Inventory inventory = context.inventory();
        CraftingGuiSupport.fill(inventory);

        String npcId = resolveNpc(context.context());
        List<CraftingRecipe> recipes = registry.getForNpc(npcId);
        int totalPages = Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
        int page = Math.max(0, Math.min(context.context().getInt(PAGE_KEY, 0), totalPages - 1));
        int start = page * RECIPES_PER_PAGE;
        int end = Math.min(recipes.size(), start + RECIPES_PER_PAGE);

        for (int index = start; index < end; index++) {
            CraftingRecipe recipe = recipes.get(index);
            inventory.setItem(index - start, createRecipeIcon(player, recipe));
        }

        if (page > 0) {
            inventory.setItem(45, CraftingGuiSupport.button(
                    Material.ARROW,
                    Component.text("前のページ", NamedTextColor.YELLOW),
                    Component.text("ページ " + page + " / " + totalPages, NamedTextColor.GRAY)
            ));
        }
        inventory.setItem(49, createProfessionIcon(player));
        inventory.setItem(50, CraftingGuiSupport.button(
                Material.CHEST,
                Component.text("製作キュー", NamedTextColor.AQUA, TextDecoration.BOLD),
                Component.text(craftingService.getJobs(player).size() + " / " + CraftingService.MAX_QUEUE_SIZE, NamedTextColor.GRAY),
                Component.text("クリックして進行状況を確認", NamedTextColor.YELLOW)
        ));
        if (page + 1 < totalPages) {
            inventory.setItem(53, CraftingGuiSupport.button(
                    Material.ARROW,
                    Component.text("次のページ", NamedTextColor.YELLOW),
                    Component.text("ページ " + (page + 2) + " / " + totalPages, NamedTextColor.GRAY)
            ));
        }
    }

    @Override
    public void onClick(GuiClickContext context) {
        Player player = context.player();
        String npcId = resolveNpc(context.context());
        int page = Math.max(0, context.context().getInt(PAGE_KEY, 0));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);

        if (context.slot() == 45 && page > 0) {
            context.open(ID, listContext(npcId, page - 1));
            return;
        }
        if (context.slot() == 50) {
            context.open(CraftingQueueGui.ID, GuiContext.builder().put(NPC_KEY, npcId).build());
            return;
        }
        if (context.slot() == 53) {
            int total = registry.getForNpc(npcId).size();
            if ((page + 1) * RECIPES_PER_PAGE < total) {
                context.open(ID, listContext(npcId, page + 1));
            }
            return;
        }
        if (context.slot() < 0 || context.slot() >= RECIPES_PER_PAGE) {
            return;
        }

        List<CraftingRecipe> recipes = registry.getForNpc(npcId);
        int index = page * RECIPES_PER_PAGE + context.slot();
        if (index >= recipes.size()) {
            return;
        }
        context.open(CraftingRecipeDetailGui.ID, GuiContext.builder()
                .put(NPC_KEY, npcId)
                .put(PAGE_KEY, page)
                .put(CraftingRecipeDetailGui.RECIPE_KEY, recipes.get(index).getId())
                .build());
    }

    private ItemStack createRecipeIcon(Player player, CraftingRecipe recipe) {
        CraftingService.CraftingAvailability availability = craftingService.getAvailability(player, recipe);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("必要製作Lv: ", NamedTextColor.GRAY)
                .append(Component.text(recipe.getRequiredCraftingLevel(),
                        availability.levelMet() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.text("製作時間: ", NamedTextColor.GRAY)
                .append(Component.text(CraftingGuiSupport.formatDuration(recipe.getCraftingTime()), NamedTextColor.YELLOW)));
        lore.add(Component.text("獲得EXP: ", NamedTextColor.GRAY)
                .append(Component.text(recipe.getProfessionExperience(), NamedTextColor.AQUA)));
        lore.add(Component.empty());
        lore.add(Component.text(
                availability.canStart() ? "製作可能" : "条件を満たしていません",
                availability.canStart() ? NamedTextColor.GREEN : NamedTextColor.RED,
                TextDecoration.BOLD
        ));
        lore.add(Component.text("クリックして素材を確認", NamedTextColor.YELLOW));
        return CraftingGuiSupport.appendLore(
                CraftingGuiSupport.customItem(itemManager, recipe.getResultItemId()),
                lore
        );
    }

    private ItemStack createProfessionIcon(Player player) {
        ProfessionService.ProfessionProgress progress = professionService.getProgress(player, ProfessionType.CRAFTING);
        String experience = progress.requiredExperience() <= 0L
                ? "MAX"
                : progress.currentExperience() + " / " + progress.requiredExperience();
        return CraftingGuiSupport.button(
                Material.CRAFTING_TABLE,
                Component.text("製作 Lv." + progress.level(), NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("EXP: " + experience, NamedTextColor.GRAY),
                Component.text("高レベルのレシピを順次解放します", NamedTextColor.DARK_GRAY)
        );
    }

    private String resolveNpc(GuiContext context) {
        String npc = context.getString(NPC_KEY);
        return npc == null || npc.isBlank() ? "合成屋" : npc;
    }

    static GuiContext listContext(String npcId, int page) {
        return GuiContext.builder().put(NPC_KEY, npcId).put(PAGE_KEY, page).build();
    }
}
