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

import java.util.List;
import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CraftingRepairConfirmGui implements GuiView {

    public static final String ID = "crafting_repair_confirm";
    public static final String INSTANCE_KEY = "repair_instance";

    private final RepairService repairService;

    @Inject
    public CraftingRepairConfirmGui(RepairService repairService) {
        this.repairService = repairService;
    }

    @Override public String getId() { return ID; }

    @Override
    public Component getTitle(Player player, GuiContext context) {
        return Component.text("装備修理の確認", NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override public int getSize(Player player, GuiContext context) { return 27; }

    @Override
    public void render(GuiRenderContext context) {
        Inventory inventory = context.inventory();
        CraftingGuiSupport.fill(inventory);
        RepairService.RepairQuote quote = quote(context.player(), context.context());
        if (quote == null) {
            inventory.setItem(13, CraftingGuiSupport.button(Material.BARRIER,
                    Component.text("修理対象が見つかりません", NamedTextColor.RED)));
        } else {
            inventory.setItem(13, CraftingGuiSupport.appendLore(quote.preview(), List.of(
                    Component.empty(),
                    Component.text("修理後: " + quote.maxDurability() + " / " + quote.maxDurability(), NamedTextColor.GREEN),
                    Component.text("修理費: " + repairService.formatMoney(quote.price()), NamedTextColor.GOLD)
            )));
            boolean affordable = repairService.getBalance(context.player()) >= quote.price();
            inventory.setItem(22, CraftingGuiSupport.button(
                    affordable ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                    Component.text(affordable ? "修理する" : "所持金不足",
                            affordable ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text("所持金: " + repairService.formatMoney(repairService.getBalance(context.player())),
                            NamedTextColor.GRAY)
            ));
        }
        inventory.setItem(18, CraftingGuiSupport.button(Material.ARROW,
                Component.text("修理一覧へ戻る", NamedTextColor.YELLOW)));
    }

    @Override
    public void onClick(GuiClickContext context) {
        String npcId = context.context().getString(CraftingRecipeListGui.NPC_KEY);
        if (context.slot() == 18) {
            context.open(CraftingRepairListGui.ID, GuiContext.builder()
                    .put(CraftingRecipeListGui.NPC_KEY, npcId).build());
            return;
        }
        if (context.slot() != 22) return;
        UUID instanceId = context.context().getUuid(INSTANCE_KEY);
        RepairService.RepairResult result = repairService.repair(context.player(), instanceId);
        if (result == RepairService.RepairResult.SUCCESS) {
            context.player().playSound(context.player().getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            context.player().sendMessage(Component.text("装備を修理しました。", NamedTextColor.GREEN));
            context.open(CraftingRepairListGui.ID, GuiContext.builder()
                    .put(CraftingRecipeListGui.NPC_KEY, npcId).build());
            return;
        }
        context.player().playSound(context.player().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
        context.player().sendMessage(Component.text(failureMessage(result), NamedTextColor.RED));
        context.refresh();
    }

    private RepairService.RepairQuote quote(Player player, GuiContext context) {
        return repairService.getQuote(player, context.getUuid(INSTANCE_KEY));
    }

    private String failureMessage(RepairService.RepairResult result) {
        return switch (result) {
            case INSUFFICIENT_FUNDS -> "修理費が不足しています。";
            case ECONOMY_UNAVAILABLE -> "経済システムを利用できません。";
            case PAYMENT_FAILED -> "修理費の支払いに失敗しました。";
            case TARGET_CHANGED -> "装備の状態が変化したため修理を中止しました。";
            case NOT_FOUND -> "修理対象が見つかりません。";
            case SUCCESS -> "";
        };
    }
}
