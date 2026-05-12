package com.ruskserver.deepwither_V2.modules.player.gui;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerAttributeProvider;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerLevelProvider;
import com.ruskserver.deepwither_V2.modules.skill.gui.SkillAssignmentGui;
import com.ruskserver.deepwither_V2.modules.skilltree.gui.SkillTreeGui;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class MainMenuGui implements Listener {

    public static final Component GUI_TITLE = Component.text("メインメニュー", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    private static final int GUI_SIZE = 54;

    private final PlayerManager playerManager;
    private final PlayerDataRepository repository;
    private final StatManager statManager;
    private final TraderService traderService;
    private final SkillTreeGui skillTreeGui;
    private final AttributeGui attributeGui;
    private final SkillAssignmentGui skillAssignmentGui;

    @Inject
    public MainMenuGui(PlayerManager playerManager, PlayerDataRepository repository,
                       StatManager statManager, TraderService traderService,
                       SkillTreeGui skillTreeGui, AttributeGui attributeGui,
                       SkillAssignmentGui skillAssignmentGui) {
        this.playerManager = playerManager;
        this.repository = repository;
        this.statManager = statManager;
        this.traderService = traderService;
        this.skillTreeGui = skillTreeGui;
        this.attributeGui = attributeGui;
        this.skillAssignmentGui = skillAssignmentGui;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        fillBackground(gui);

        gui.setItem(9, createProfileIcon(player));
        gui.setItem(10, createCombatStatsIcon(player));
        gui.setItem(11, createAttributeSummaryIcon(player));

        gui.setItem(37, createNavButton(
                Material.BOOK,
                Component.text("スキルツリー", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("スキルポイントを消費して", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("新しい能力を習得します。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("▶ クリックして開く", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));

        gui.setItem(39, createNavButton(
                Material.NETHER_STAR,
                Component.text("能力値 (Attributes)", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("ステータスポイントを割り振り", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("基礎能力を強化します。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("▶ クリックして開く", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));

        gui.setItem(41, createNavButton(
                Material.WRITABLE_BOOK,
                Component.text("スキルセット", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("習得したスキルを", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("スロットに装備します。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("▶ クリックして開く", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));

        gui.setItem(43, createNavButton(
                Material.PAPER,
                Component.text("ステータス詳細", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("現在のステータスを", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("詳細に確認します。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("▶ クリックして表示", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));

        gui.setItem(49, createNavButton(
                Material.BARRIER,
                Component.text("閉じる", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                Component.text("メニューを閉じます。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));

        player.openInventory(gui);
    }

    private ItemStack createProfileIcon(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text("[ 基本情報 ]", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        repository.get(player.getUniqueId()).ifPresent(data -> {
            PlayerLevelProvider.LevelData levelData = data.get(PlayerLevelProvider.KEY);
            if (levelData != null) {
                int level = levelData.getLevel();
                int exp = levelData.getExp();
                int nextExp = playerManager.getExpToNextLevel(level);
                double percent = nextExp > 0 && nextExp != Integer.MAX_VALUE
                        ? (double) exp / nextExp * 100 : 0.0;
                lore.add(Component.text(" Level: ", NamedTextColor.GRAY)
                        .append(Component.text(level, NamedTextColor.GREEN))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(" Exp: ", NamedTextColor.GRAY)
                        .append(Component.text(String.format("%.1f%%", percent), NamedTextColor.YELLOW))
                        .decoration(TextDecoration.ITALIC, false));
            }
        });

        lore.add(Component.text(" Money: ", NamedTextColor.GRAY)
                .append(Component.text(traderService.formatMoney(traderService.getBalance(player)), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(Component.text("プレイヤーの基本情報です。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCombatStatsIcon(Player player) {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("[ 戦闘ステータス ]", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        double currentHp = player.getHealth();
        double maxHp = statManager.getTotalStat(player, StatType.HEALTH);
        lore.add(Component.text(" HP: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.0f", currentHp), NamedTextColor.WHITE))
                .append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.0f", maxHp), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(statLine("攻撃力", statManager.getTotalStat(player, StatType.ATTACK_DAMAGE), NamedTextColor.RED, false));
        lore.add(statLine("防御力", statManager.getTotalStat(player, StatType.DEFENSE), NamedTextColor.BLUE, false));
        lore.add(statLine("魔法攻撃力", statManager.getTotalStat(player, StatType.MAGIC_DAMAGE), NamedTextColor.LIGHT_PURPLE, false));
        lore.add(statLine("魔法防御力", statManager.getTotalStat(player, StatType.MAGIC_DEFENSE), NamedTextColor.DARK_AQUA, false));
        lore.add(statLine("クリ率", statManager.getTotalStat(player, StatType.CRITICAL_CHANCE) * 100, NamedTextColor.GOLD, true));
        lore.add(statLine("クリ倍率", statManager.getTotalStat(player, StatType.CRITICAL_DAMAGE) * 100, NamedTextColor.GOLD, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributeSummaryIcon(Player player) {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("[ 属性ポイント ]", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        repository.get(player.getUniqueId()).ifPresent(data -> {
            PlayerAttributeProvider.AttributeData attrData = data.get(PlayerAttributeProvider.KEY);
            if (attrData != null) {
                lore.add(Component.text(" 残りポイント: ", NamedTextColor.GRAY)
                        .append(Component.text(attrData.getRemainingPoints(), NamedTextColor.AQUA))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                for (AttributeType type : AttributeType.values()) {
                    lore.add(Component.text(" " + type.getDisplayName() + ": ", NamedTextColor.GRAY)
                            .append(Component.text(attrData.getAttribute(type), NamedTextColor.GREEN))
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
        });

        lore.add(Component.empty());
        lore.add(Component.text("「能力値」から割り振れます。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavButton(Material material, Component name, Component... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        List<Component> nonItalicLore = new ArrayList<>();
        for (Component line : loreLines) {
            nonItalicLore.add(line.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(nonItalicLore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        glass.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, glass);
            }
        }
    }

    private Component statLine(String name, double value, NamedTextColor color, boolean isPercent) {
        String valStr = isPercent ? String.format("%.1f%%", value) : String.format("%.0f", value);
        return Component.text("  ", NamedTextColor.GRAY)
                .append(Component.text(name + ": ", NamedTextColor.GRAY))
                .append(Component.text(valStr, color))
                .decoration(TextDecoration.ITALIC, false);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getSlot();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        switch (slot) {
            case 37 -> {
                player.closeInventory();
                skillTreeGui.openSelectorOrFirst(player);
            }
            case 39 -> {
                player.closeInventory();
                attributeGui.open(player);
            }
            case 41 -> {
                player.closeInventory();
                skillAssignmentGui.open(player);
            }
            case 43 -> {
                player.closeInventory();
                player.performCommand("status");
            }
            case 49 -> player.closeInventory();
        }
    }
}
