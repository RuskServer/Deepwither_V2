package com.ruskserver.deepwither_V2.modules.player.gui;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.gui.GuiClickContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiRenderContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiView;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.provider.CharacterAttributeProvider;
import com.ruskserver.deepwither_V2.modules.player.provider.CharacterLevelProvider;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class MainMenuGui implements GuiView {

    public static final Component GUI_TITLE = Component.text("メインメニュー", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    public static final String ID = "main_menu";
    private static final int GUI_SIZE = 54;

    private final PlayerManager playerManager;
    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final StatManager statManager;
    private final TraderService traderService;

    @Inject
    public MainMenuGui(PlayerManager playerManager, CharacterDataRepository characterDataRepository,
                       CharacterService characterService, StatManager statManager, TraderService traderService) {
        this.playerManager = playerManager;
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.statManager = statManager;
        this.traderService = traderService;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getTitle(Player player, GuiContext context) {
        return GUI_TITLE;
    }

    @Override
    public int getSize(Player player, GuiContext context) {
        return GUI_SIZE;
    }

    @Override
    public void render(GuiRenderContext context) {
        Player player = context.player();
        Inventory gui = context.inventory();
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

        gui.setItem(45, createNavButton(
                Material.CHEST,
                Component.text("パーティー管理", NamedTextColor.DARK_GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("パーティーを作成・管理します。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("フレンドと一緒に冒険しましょう！", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("▶ クリックして開く", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));

        gui.setItem(49, createNavButton(
                Material.BARRIER,
                Component.text("閉じる", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                Component.text("メニューを閉じます。", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));

    }

    private ItemStack createProfileIcon(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text("[ 基本情報 ]", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        characterService.getActiveCharacter(player.getUniqueId()).ifPresent(c -> {
            characterDataRepository.get(c.characterId()).ifPresent(data -> {
                CharacterLevelProvider.LevelData levelData = data.get(CharacterLevelProvider.KEY);
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

        characterService.getActiveCharacter(player.getUniqueId()).ifPresent(c -> {
            characterDataRepository.get(c.characterId()).ifPresent(data -> {
                CharacterAttributeProvider.AttributeData attrData = data.get(CharacterAttributeProvider.KEY);
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

    @Override
    public void onClick(GuiClickContext context) {
        Player player = context.player();
        ItemStack clicked = context.currentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = context.slot();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        switch (slot) {
            case 37 -> context.open("skilltree");
            case 39 -> context.open(AttributeGui.ID);
            case 41 -> context.open("skill_assignment");
            case 43 -> {
                context.close();
                player.performCommand("status");
            }
            case 45 -> context.open("party");
            case 49 -> context.close();
        }
    }
}
