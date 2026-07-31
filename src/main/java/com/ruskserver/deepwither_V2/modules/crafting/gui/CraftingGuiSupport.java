package com.ruskserver.deepwither_V2.modules.crafting.gui;

import com.ruskserver.deepwither_V2.modules.gui.GuiItemBuilder;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class CraftingGuiSupport {

    private CraftingGuiSupport() {
    }

    static ItemStack customItem(ItemManager itemManager, String itemId) {
        ItemStack item = itemManager.generate(itemId);
        return item != null ? item : GuiItemBuilder.of(Material.BARRIER)
                .name(Component.text("未定義アイテム: " + itemId, NamedTextColor.RED))
                .build();
    }

    static ItemStack appendLore(ItemStack item, List<Component> additions) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.lore() == null
                ? new ArrayList<>()
                : new ArrayList<>(meta.lore());
        additions.stream()
                .map(line -> line.decoration(TextDecoration.ITALIC, false))
                .forEach(lore::add);
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack button(Material material, Component name, Component... lore) {
        return GuiItemBuilder.of(material)
                .name(name)
                .lore(lore)
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build();
    }

    static void fill(Inventory inventory) {
        ItemStack background = GuiItemBuilder.background(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, background);
        }
    }

    static String formatDuration(Duration duration) {
        long seconds = Math.max(0L, duration.toSeconds());
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        return minutes > 0L ? minutes + "分" + remainingSeconds + "秒" : seconds + "秒";
    }

    static String formatRemaining(long completionTimeMillis) {
        long millis = Math.max(0L, completionTimeMillis - System.currentTimeMillis());
        return formatDuration(Duration.ofMillis(millis));
    }
}
