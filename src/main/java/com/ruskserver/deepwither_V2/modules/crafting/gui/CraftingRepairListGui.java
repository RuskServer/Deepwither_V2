package com.ruskserver.deepwither_V2.modules.crafting.gui;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.crafting.service.RepairService;
import com.ruskserver.deepwither_V2.modules.gui.GuiClickContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiRenderContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiView;
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
public class CraftingRepairListGui implements GuiView {

    public static final String ID = "crafting_repairs";
    private static final int ITEM_LIMIT = 45;

    private final RepairService repairService;

    @Inject
    public CraftingRepairListGui(RepairService repairService) {
        this.repairService = repairService;
    }

    @Override public String getId() { return ID; }

    @Override
    public Component getTitle(Player player, GuiContext context) {
        return Component.text("合成屋 - 装備修理", NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override public int getSize(Player player, GuiContext context) { return 54; }

    @Override
    public void render(GuiRenderContext context) {
        Inventory inventory = context.inventory();
        CraftingGuiSupport.fill(inventory);
        List<RepairService.RepairQuote> quotes = repairService.getRepairableItems(context.player());
        for (int index = 0; index < Math.min(ITEM_LIMIT, quotes.size()); index++) {
            inventory.setItem(index, icon(quotes.get(index)));
        }
        if (quotes.isEmpty()) {
            inventory.setItem(22, CraftingGuiSupport.button(Material.LIME_STAINED_GLASS_PANE,
                    Component.text("修理が必要な装備はありません", NamedTextColor.GREEN)));
        }
        inventory.setItem(45, CraftingGuiSupport.button(Material.ARROW,
                Component.text("製作レシピへ戻る", NamedTextColor.YELLOW)));
        inventory.setItem(49, CraftingGuiSupport.button(Material.GOLD_INGOT,
                Component.text("所持金: " + repairService.formatMoney(repairService.getBalance(context.player())),
                        NamedTextColor.GOLD)));
    }

    @Override
    public void onClick(GuiClickContext context) {
        String npcId = context.context().getString(CraftingRecipeListGui.NPC_KEY);
        if (context.slot() == 45) {
            context.open(CraftingRecipeListGui.ID, CraftingRecipeListGui.listContext(npcId, 0));
            return;
        }
        if (context.slot() < 0 || context.slot() >= ITEM_LIMIT) return;
        List<RepairService.RepairQuote> quotes = repairService.getRepairableItems(context.player());
        if (context.slot() >= quotes.size()) return;
        RepairService.RepairQuote quote = quotes.get(context.slot());
        context.player().playSound(context.player().getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
        context.open(CraftingRepairConfirmGui.ID, GuiContext.builder()
                .put(CraftingRecipeListGui.NPC_KEY, npcId)
                .put(CraftingRepairConfirmGui.INSTANCE_KEY, quote.instanceId())
                .build());
    }

    private ItemStack icon(RepairService.RepairQuote quote) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("耐久値: " + quote.remainingDurability() + " / " + quote.maxDurability(),
                quote.broken() ? NamedTextColor.RED : NamedTextColor.GRAY));
        lore.add(Component.text("修理費: " + repairService.formatMoney(quote.price()), NamedTextColor.GOLD));
        lore.add(Component.text("クリックして修理内容を確認", NamedTextColor.YELLOW));
        return CraftingGuiSupport.appendLore(quote.preview(), lore);
    }
}
