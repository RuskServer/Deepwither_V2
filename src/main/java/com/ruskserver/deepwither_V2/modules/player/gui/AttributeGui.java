package com.ruskserver.deepwither_V2.modules.player.gui;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.modules.gui.GuiClickContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiRenderContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiView;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerAttributeProvider;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Component
public class AttributeGui implements GuiView {

    public static final net.kyori.adventure.text.Component GUI_TITLE = net.kyori.adventure.text.Component.text("ステータス割り振り", NamedTextColor.DARK_GREEN);

    public static final String ID = "attributes";
    private static final int GUI_SIZE = 27;

    private final PlayerManager playerManager;
    private final PlayerDataRepository repository;

    @Inject
    public AttributeGui(PlayerManager playerManager, PlayerDataRepository repository) {
        this.playerManager = playerManager;
        this.repository = repository;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public net.kyori.adventure.text.Component getTitle(Player player, GuiContext context) {
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
        repository.get(player.getUniqueId()).ifPresent(data -> {
            var attrData = data.get(PlayerAttributeProvider.KEY);

            AttributeType[] guiStats = { AttributeType.STR, AttributeType.VIT, AttributeType.MND, AttributeType.INT, AttributeType.AGI };

            for (AttributeType type : guiStats) {
                ItemStack icon = getStatIcon(type, attrData);
                int slot = switch (type) {
                    case STR -> 9;
                    case VIT -> 11;
                    case MND -> 13;
                    case INT -> 15;
                    case AGI -> 17;
                };
                gui.setItem(slot, icon);
            }

            gui.setItem(22, createBackButton());
        });
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("戻る", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(net.kyori.adventure.text.Component.text("前のメニューへ戻ります", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getStatIcon(AttributeType type, PlayerAttributeProvider.AttributeData attrData) {
        Material mat = switch (type) {
            case STR -> Material.IRON_SWORD;
            case VIT -> Material.GOLDEN_APPLE;
            case MND -> Material.POTION;
            case INT -> Material.BOOK;
            case AGI -> Material.LEATHER_BOOTS;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text(type.getDisplayName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

        int currentLevel = attrData.getAttribute(type);
        int remainingPoints = attrData.getRemainingPoints();

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(net.kyori.adventure.text.Component.text("現在の " + type.getDisplayName() + " レベル: ", NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(currentLevel, NamedTextColor.GOLD, TextDecoration.BOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(net.kyori.adventure.text.Component.text("効果:", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

        for (String buff : getBuffDescription(type, currentLevel)) {
            lore.add(net.kyori.adventure.text.Component.text("  ✤ ", NamedTextColor.GRAY).append(net.kyori.adventure.text.Component.text(buff, NamedTextColor.BLUE))
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(net.kyori.adventure.text.Component.text("クリックで1ポイント消費してレベルアップする。", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(net.kyori.adventure.text.Component.text("◆ 現在所持: ", NamedTextColor.YELLOW)
                .append(net.kyori.adventure.text.Component.text(remainingPoints + " ポイント", NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> getBuffDescription(AttributeType type, int level) {
        List<String> list = new ArrayList<>();
        switch (type) {
            case STR -> list.add("+ " + (level * 1) + "% 攻撃力");
            case VIT -> {
                list.add("+ " + (level * 1) + "% 最大HP");
                list.add("+ " + (level * 0.5) + "% 防御力");
            }
            case MND -> {
                list.add("+ " + (level * 1.5) + "% クリティカルダメージ");
                list.add("+ " + (level * 1.5) + "% 魔法ダメージ");
            }
            case INT -> {
                list.add("+ " + (level * 2) + "% 最大マナ");
            }
            case AGI -> {
                list.add("+ " + String.format("%.1f", level * 0.2) + "% クリティカル率");
            }
        }
        return list;
    }

    @Override
    public void onClick(GuiClickContext context) {
        Player player = context.player();
        int slot = context.slot();
        if (slot == 22) {
            context.back();
            return;
        }
        AttributeType type = switch (slot) {
            case 9 -> AttributeType.STR;
            case 11 -> AttributeType.VIT;
            case 13 -> AttributeType.MND;
            case 15 -> AttributeType.INT;
            case 17 -> AttributeType.AGI;
            default -> null;
        };

        if (type == null) return;

        boolean success = playerManager.addAttributePoint(player, type);
        if (success) {
            player.sendMessage(net.kyori.adventure.text.Component.text(type.getDisplayName() + " に 1ポイント割り振りました！", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            context.refresh();
        } else {
            player.sendMessage(net.kyori.adventure.text.Component.text("ポイントが足りないか、上限に達しています！", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
