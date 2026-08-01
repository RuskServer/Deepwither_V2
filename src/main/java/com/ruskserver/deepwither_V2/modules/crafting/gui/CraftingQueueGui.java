package com.ruskserver.deepwither_V2.modules.crafting.gui;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.crafting.CraftingJob;
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
import java.util.List;
import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CraftingQueueGui implements GuiView {

    public static final String ID = "crafting_queue";
    private static final int JOB_SLOT_LIMIT = CraftingService.MAX_QUEUE_SIZE;

    private final CraftingService craftingService;
    private final ItemManager itemManager;

    @Inject
    public CraftingQueueGui(CraftingService craftingService, ItemManager itemManager) {
        this.craftingService = craftingService;
        this.itemManager = itemManager;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getTitle(Player player, GuiContext context) {
        return Component.text("製作キュー", NamedTextColor.DARK_AQUA)
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
        List<CraftingJob> jobs = orderedJobs(context.player());
        for (int index = 0; index < Math.min(jobs.size(), JOB_SLOT_LIMIT); index++) {
            inventory.setItem(index, createJobIcon(jobs.get(index)));
        }
        if (jobs.isEmpty()) {
            inventory.setItem(4, CraftingGuiSupport.button(
                    Material.CLOCK,
                    Component.text("製作キューは空です", NamedTextColor.GRAY),
                    Component.text("レシピ一覧から製作を開始できます。", NamedTextColor.DARK_GRAY)
            ));
        }
        inventory.setItem(18, CraftingGuiSupport.button(
                Material.ARROW,
                Component.text("レシピ一覧へ戻る", NamedTextColor.YELLOW)
        ));
        long finished = jobs.stream().filter(CraftingJob::isFinished).count();
        inventory.setItem(22, CraftingGuiSupport.button(
                finished > 0 ? Material.HOPPER : Material.GRAY_DYE,
                Component.text("完成品をすべて受け取る",
                        finished > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                        TextDecoration.BOLD),
                Component.text("受取可能: " + finished + "件", NamedTextColor.YELLOW)
        ));
        inventory.setItem(26, CraftingGuiSupport.button(
                Material.CHEST,
                Component.text("キュー状況", NamedTextColor.AQUA, TextDecoration.BOLD),
                Component.text("完成: " + finished + "件", NamedTextColor.GREEN),
                Component.text("製作中: " + (jobs.size() - finished) + "件", NamedTextColor.YELLOW),
                Component.text("使用中: " + jobs.size() + " / " + CraftingService.MAX_QUEUE_SIZE, NamedTextColor.GRAY)
        ));
    }

    @Override
    public void onClick(GuiClickContext context) {
        String npcId = context.context().getString(CraftingRecipeListGui.NPC_KEY);
        if (context.slot() == 18) {
            context.open(CraftingRecipeListGui.ID, CraftingRecipeListGui.listContext(npcId, 0));
            return;
        }
        if (context.slot() == 22) {
            claimAllFinished(context);
            context.rerender();
            return;
        }
        if (context.slot() < 0 || context.slot() >= JOB_SLOT_LIMIT) {
            return;
        }
        List<CraftingJob> jobs = orderedJobs(context.player());
        if (context.slot() >= jobs.size()) {
            return;
        }
        CraftingJob job = jobs.get(context.slot());
        CraftingService.ClaimResult result = craftingService.claim(context.player(), job.getJobId());
        if (result == CraftingService.ClaimResult.SUCCESS) {
            context.player().playSound(context.player().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
            context.player().sendMessage(Component.text("完成品を受け取りました。", NamedTextColor.GOLD));
        } else if (result == CraftingService.ClaimResult.NOT_FINISHED) {
            context.player().sendMessage(Component.text("まだ製作中です。", NamedTextColor.YELLOW));
        } else if (result == CraftingService.ClaimResult.INVENTORY_FULL) {
            context.player().sendMessage(Component.text("インベントリに空きがありません。", NamedTextColor.RED));
        } else {
            context.player().sendMessage(Component.text("完成品を受け取れませんでした。", NamedTextColor.RED));
        }
        context.rerender();
    }

    private void claimAllFinished(GuiClickContext context) {
        CraftingService.ClaimAllResult result = craftingService.claimAllFinished(context.player());
        if (result.noActiveCharacter()) {
            context.player().sendMessage(Component.text("有効なキャラクターが選択されていません。", NamedTextColor.RED));
            return;
        }
        if (result.claimedCount() > 0) {
            context.player().playSound(context.player().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.3f);
            context.player().sendMessage(Component.text(
                    "完成品を" + result.claimedCount() + "件受け取りました。", NamedTextColor.GOLD));
        } else {
            context.player().playSound(context.player().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            context.player().sendMessage(Component.text("受け取れる完成品がありません。", NamedTextColor.YELLOW));
        }
        if (result.inventoryBlockedCount() > 0) {
            context.player().sendMessage(Component.text(
                    "インベントリ不足で" + result.inventoryBlockedCount() + "件を残しました。", NamedTextColor.RED));
        }
        if (result.invalidResultCount() > 0) {
            context.player().sendMessage(Component.text(
                    "結果アイテムを生成できない製作が" + result.invalidResultCount() + "件あります。", NamedTextColor.RED));
        }
    }

    private List<CraftingJob> orderedJobs(Player player) {
        return craftingService.getJobs(player).stream()
                .sorted(Comparator.comparing(CraftingJob::isFinished).reversed()
                        .thenComparingLong(CraftingJob::getCompletionTimeMillis))
                .toList();
    }

    private ItemStack createJobIcon(CraftingJob job) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (job.isFinished()) {
            lore.add(Component.text("完成", NamedTextColor.GREEN, TextDecoration.BOLD));
            lore.add(Component.text("クリックして受け取る", NamedTextColor.YELLOW));
            lore.add(Component.text("獲得製作EXP: " + job.getProfessionExperience(), NamedTextColor.AQUA));
        } else {
            lore.add(Component.text("製作中", NamedTextColor.YELLOW, TextDecoration.BOLD));
            lore.add(Component.text("残り: " + CraftingGuiSupport.formatRemaining(job.getCompletionTimeMillis()), NamedTextColor.GRAY));
        }
        if (!job.getAdditionalResults().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("完成時に返却される余剰素材:", NamedTextColor.AQUA));
            job.getAdditionalResults().forEach((itemId, amount) -> lore.add(
                    Component.text("- " + displayName(itemId) + " x" + amount, NamedTextColor.GRAY)));
        }
        ItemStack item = CraftingGuiSupport.customItem(itemManager, job.getResultItemId());
        item.setAmount(Math.min(item.getMaxStackSize(), Math.max(1, job.getResultAmount())));
        return CraftingGuiSupport.appendLore(item, lore);
    }

    private String displayName(String itemId) {
        var definition = itemManager.getCustomItem(itemId);
        return definition == null ? itemId : definition.getDisplayName();
    }
}
