package com.ruskserver.deepwither_V2.modules.party;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.player.gui.MainMenuGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class PartyTagGUI implements Listener {

    private static final Component TITLE = Component.text("§8募集設定");
    private static final int[] TAG_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    private final PartyManager partyManager;
    private final MainMenuGui mainMenuGui;

    @Inject
    public PartyTagGUI(PartyManager partyManager, MainMenuGui mainMenuGui) {
        this.partyManager = partyManager;
        this.mainMenuGui = mainMenuGui;
    }

    public void open(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("§cパーティーリーダーのみが設定できます。", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBackground(inv);

        inv.setItem(4, createItem(Material.BOOK, "§b募集設定",
                "§7タグを1つ以上選択してください",
                "§7最大メンバー数を設定できます (2-6)"));

        PartyTag[] tags = PartyTag.values();
        for (int i = 0; i < TAG_SLOTS.length && i < tags.length; i++) {
            int slot = TAG_SLOTS[i];
            PartyTag tag = tags[i];
            boolean selected = party.getTags().contains(tag);

            ItemStack item = new ItemStack(tag.getIcon());
            ItemMeta meta = item.getItemMeta();

            if (selected) {
                meta.displayName(Component.text("§a▶ 選択中 " + tag.getDisplayName())
                        .decoration(TextDecoration.ITALIC, false));
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.displayName(Component.text("§7▷ 未選択 " + tag.getDisplayName())
                        .decoration(TextDecoration.ITALIC, false));
            }

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7カラー: ").decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(tag.getColor() + tag.getColor().toString())));
            lore.add(Component.text(""));
            lore.add(Component.text(selected ? "§aクリックで解除" : "§7クリックで選択")
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }

        ItemStack maxItem = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta maxMeta = maxItem.getItemMeta();
        maxMeta.displayName(Component.text("§b最大メンバー数: §e" + party.getMaxMembers())
                .decoration(TextDecoration.ITALIC, false));
        List<Component> maxLore = new ArrayList<>();
        maxLore.add(Component.text("§7左クリック: +1 右クリック: -1").decoration(TextDecoration.ITALIC, false));
        maxLore.add(Component.text("§7範囲: 2〜6").decoration(TextDecoration.ITALIC, false));
        maxMeta.lore(maxLore);
        maxItem.setItemMeta(maxMeta);
        inv.setItem(40, maxItem);

        inv.setItem(48, createItem(Material.RED_DYE, "§cキャンセル",
                "§7/party gui で開き直せます"));

        inv.setItem(50, createItem(Material.LIME_DYE, "§a確定して公開",
                "§7タグを設定してパーティーを公開"));

        inv.setItem(52, createItem(Material.ARROW, "§7メインメニュー"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        Party party = partyManager.getParty(player);
        if (party == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();

        if (slot == 48) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            return;
        }

        if (slot == 50) {
            if (party.getTags().isEmpty()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                player.sendMessage(Component.text("§cタグを1つ以上選択してください。", NamedTextColor.RED));
                return;
            }
            partyManager.setPartyPublic(player, true);
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            return;
        }

        if (slot == 52) {
            player.closeInventory();
            mainMenuGui.open(player);
            return;
        }

        if (slot == 40) {
            int current = party.getMaxMembers();
            if (event.getClick() == ClickType.LEFT && current < 6) {
                party.setMaxMembers(current + 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            } else if (event.getClick() == ClickType.RIGHT && current > 2) {
                party.setMaxMembers(current - 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
            open(player);
            return;
        }

        for (int i = 0; i < TAG_SLOTS.length && i < PartyTag.values().length; i++) {
            if (slot == TAG_SLOTS[i]) {
                PartyTag tag = PartyTag.values()[i];
                if (party.getTags().contains(tag)) {
                    party.getTags().remove(tag);
                } else {
                    party.getTags().add(tag);
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                open(player);
                return;
            }
        }
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }
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
}
