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
    private static final int JOB_SLOT_LIMIT = 45;

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
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        Inventory inventory = context.inventory();
        CraftingGuiSupport.fill(inventory);
        List<CraftingJob> jobs = orderedJobs(context.player());
        for (int index = 0; index < Math.min(jobs.size(), JOB_SLOT_LIMIT); index++) {
            inventory.setItem(index, createJobIcon(jobs.get(index)));
        }
        inventory.setItem(45, CraftingGuiSupport.button(
                Material.ARROW,
                Component.text("レシピ一覧へ戻る", NamedTextColor.YELLOW)
        ));
        inventory.setItem(49, CraftingGuiSupport.button(
                Material.CLOCK,
                Component.text("表示を更新", NamedTextColor.AQUA),
                Component.text("クリックして残り時間を更新", NamedTextColor.GRAY)
        ));
    }

    @Override
    public void onClick(GuiClickContext context) {
        String npcId = context.context().getString(CraftingRecipeListGui.NPC_KEY);
        if (context.slot() == 45) {
            context.open(CraftingRecipeListGui.ID, CraftingRecipeListGui.listContext(npcId, 0));
            return;
        }
        if (context.slot() == 49) {
            context.player().playSound(context.player().getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            context.refresh();
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
        context.refresh();
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
        ItemStack item = CraftingGuiSupport.customItem(itemManager, job.getResultItemId());
        item.setAmount(Math.min(item.getMaxStackSize(), Math.max(1, job.getResultAmount())));
        return CraftingGuiSupport.appendLore(item, lore);
    }
}
