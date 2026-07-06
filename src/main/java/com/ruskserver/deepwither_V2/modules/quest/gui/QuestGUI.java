package com.ruskserver.deepwither_V2.modules.quest.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.quest.api.Quest;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestState;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider.QuestProgress;
import com.ruskserver.deepwither_V2.modules.quest.service.QuestService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class QuestGUI implements Listener {

    private final QuestService questService;
    private final ItemPDCUtil pdcUtil;
    private final NamespacedKey actionKey;

    @Inject
    public QuestGUI(QuestService questService, ItemPDCUtil pdcUtil, Deepwither_V2 plugin) {
        this.questService = questService;
        this.pdcUtil = pdcUtil;
        this.actionKey = new NamespacedKey(plugin, "quest_gui_action");
    }

    public void openQuestGui(Player player) {
        int remaining = questService.getRemainingDailyCompletions(player.getUniqueId());
        if (remaining <= 0) {
            player.sendMessage(Component.text("今日のクエスト受注可能回数（5回）に達しました。また明日来てください。", NamedTextColor.GRAY));
            return;
        }

        QuestProgress progress = questService.getProgress(player.getUniqueId());

        if (progress.isEmpty() || progress.state() == QuestState.TURNED_IN) {
            openAcceptGui(player, remaining);
        } else if (progress.state() == QuestState.ACCEPTED) {
            if (questService.checkCompletion(player)) {
                openCompleteGui(player);
            } else {
                openProgressGui(player);
            }
        }
    }

    private void openAcceptGui(Player player, int remaining) {
        Quest quest = questService.getQuest("daily_collection");
        if (quest == null) return;

        QuestHolder holder = new QuestHolder("daily_collection");
        Inventory gui = Bukkit.createInventory(holder, 27, Component.text("§2村長の依頼"));
        holder.setInventory(gui);

        gui.setItem(11, createInfoDisplay(quest));
        gui.setItem(15, createAcceptButton());
        gui.setItem(22, createRemainingDisplay(remaining));
        gui.setItem(26, createCloseButton());

        player.openInventory(gui);
    }

    private void openProgressGui(Player player) {
        QuestProgress progress = questService.getProgress(player.getUniqueId());
        Quest quest = questService.getCurrentQuest(player);
        if (quest == null) return;

        Map<String, Integer> counts = questService.getCurrentCounts(player);

        QuestHolder holder = new QuestHolder(progress.questId());
        Inventory gui = Bukkit.createInventory(holder, 27, Component.text("§eクエスト進捗"));
        holder.setInventory(gui);

        int slot = 10;
        for (QuestObjective obj : quest.getObjectives()) {
            int has = counts.getOrDefault(obj.getItemId(), 0);
            boolean done = has >= obj.getRequiredAmount();
            gui.setItem(slot++, createObjectiveDisplay(obj, has, done));
        }

        gui.setItem(26, createCloseButton());
        player.openInventory(gui);
    }

    private void openCompleteGui(Player player) {
        QuestHolder holder = new QuestHolder("daily_collection");
        Inventory gui = Bukkit.createInventory(holder, 27, Component.text("§aクエスト完了"));
        holder.setInventory(gui);

        ItemStack complete = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = complete.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§a§l完了報告"));
            meta.lore(List.of(
                    Component.text("§7必要アイテムをすべて集めました。"),
                    Component.text("§eクリックで報告し、報酬を受け取る。")
            ));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "turn_in");
            complete.setItemMeta(meta);
        }
        gui.setItem(13, complete);
        gui.setItem(26, createCloseButton());

        player.openInventory(gui);
    }

    private ItemStack createInfoDisplay(Quest quest) {
        ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = info.getItemMeta();
        if (meta == null) return info;

        meta.displayName(Component.text("§b§l村長の収集依頼"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7村の周辺で手に入る素材を集めてきてほしい。"));
        lore.add(Component.text("§7報酬としてダンジョンへの地図を渡そう。"));
        lore.add(Component.empty());
        lore.add(Component.text("§6必要アイテム:").decoration(TextDecoration.ITALIC, false));
        for (QuestObjective obj : quest.getObjectives()) {
            String name = getItemDisplayName(obj.getItemId());
            lore.add(Component.text("§7・" + name + " x" + obj.getRequiredAmount()));
        }
        lore.add(Component.empty());
        meta.lore(lore);
        info.setItemMeta(meta);
        return info;
    }

    private ItemStack createAcceptButton() {
        ItemStack btn = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = btn.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§a§l依頼を受ける"));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "accept");
            btn.setItemMeta(meta);
        }
        return btn;
    }

    private ItemStack createRemainingDisplay(int remaining) {
        ItemStack display = new ItemStack(Material.CLOCK);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§7今日の残り受注回数: §e" + remaining + "§7/§e5"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§71日に" + remaining + "回までクエストを遂行できます。"));
            meta.lore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack createCloseButton() {
        ItemStack btn = new ItemStack(Material.BARRIER);
        ItemMeta meta = btn.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§c閉じる"));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "close");
            btn.setItemMeta(meta);
        }
        return btn;
    }

    private ItemStack createObjectiveDisplay(QuestObjective obj, int has, boolean done) {
        Material mat = done ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack display = new ItemStack(mat);
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String name = getItemDisplayName(obj.getItemId());
        meta.displayName(Component.text((done ? "§a" : "§7") + name));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text((done ? "§a完了" : "§c未完了") + " §7(" + has + "/" + obj.getRequiredAmount() + ")"));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof QuestHolder)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String action = clicked.getItemMeta().getPersistentDataContainer()
                .get(actionKey, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "accept" -> {
                questService.acceptQuest(player, "daily_collection");
                player.closeInventory();
            }
            case "turn_in" -> {
                questService.turnInQuest(player);
                player.closeInventory();
            }
            case "close" -> player.closeInventory();
        }
    }

    private String getItemDisplayName(String itemId) {
        return switch (itemId) {
            case "ghoul_viscera" -> "§2グールの臓";
            case "ghoul_remnant" -> "§8グールの残滓";
            case "moonlight_residue" -> "§b月光の残滓";
            case "abyss_shard" -> "§5深淵の欠片";
            case "void_star_dust" -> "§d虚星の塵";
            case "ghoul_essence" -> "§aグールの精髄";
            default -> itemId;
        };
    }

    public static class QuestHolder implements InventoryHolder {
        private final String questId;
        private Inventory inventory;

        public QuestHolder(String questId) {
            this.questId = questId;
        }

        public String getQuestId() { return questId; }

        @Override
        public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
}
