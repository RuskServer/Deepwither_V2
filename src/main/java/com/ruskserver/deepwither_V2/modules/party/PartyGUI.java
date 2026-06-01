package com.ruskserver.deepwither_V2.modules.party;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class PartyGUI implements Listener {

    private static final Component TITLE = Component.text("§8パーティー管理");
    private static final int PARTIES_PER_PAGE = 7;

    private final PartyManager partyManager;
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    @Inject
    public PartyGUI(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBackground(inv);

        Party party = partyManager.getParty(player);

        if (party != null) {
            inv.setItem(10, createPlayerHead(player, "§eあなたのパーティー",
                    "§7リーダー: " + getName(party.getLeaderId()),
                    "§7メンバー: " + party.getSize() + "/" + party.getMaxMembers()));

            if (party.isLeader(player.getUniqueId())) {
                if (party.isPublic()) {
                    inv.setItem(11, createItem(Material.REDSTONE_BLOCK, "§c公開中を停止",
                            "§7クリックで非公開に戻す"));
                } else {
                    inv.setItem(11, createItem(Material.EMERALD_BLOCK, "§a公開する",
                            "§7クリックで公開設定を開く"));
                }
                inv.setItem(13, createItem(Material.BARRIER, "§cパーティーを解散",
                        "§7クリックでパーティーを解散"));
            } else {
                inv.setItem(11, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
                inv.setItem(13, createItem(Material.RED_BED, "§cパーティーから脱退",
                        "§7クリックでパーティーを脱退"));
            }
        } else {
            inv.setItem(10, createItem(Material.PLAYER_HEAD, "§7パーティー未所属",
                    "§7参加すると情報が表示されます"));
            inv.setItem(11, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            inv.setItem(13, createItem(Material.GREEN_BANNER, "§aパーティーを作成",
                    "§7クリックで新規作成"));
        }

        inv.setItem(12, createItem(Material.BOOK, "§b更新", "§7クリックで再表示"));

        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        List<Party> publicParties = partyManager.getPublicParties();
        int totalPages = Math.max(1, (int) Math.ceil((double) publicParties.size() / PARTIES_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPage.put(player.getUniqueId(), page);

        int startIndex = page * PARTIES_PER_PAGE;
        for (int i = 0; i < PARTIES_PER_PAGE; i++) {
            int slot = 19 + i;
            int partyIndex = startIndex + i;
            if (partyIndex < publicParties.size()) {
                Party p = publicParties.get(partyIndex);
                if (p.getLeaderId() != null && partyManager.getParty(player) == null) {
                    inv.setItem(slot, createPartyListingItem(p));
                }
            }
        }

        if (publicParties.isEmpty() && party == null) {
            inv.setItem(29, createItem(Material.PAPER, "§7公開パーティーはありません",
                    "§7/party create で作成しよう"));
        }

        if (page > 0) {
            inv.setItem(47, createItem(Material.ARROW, "§e← 前のページ"));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createItem(Material.ARROW, "§e次のページ →"));
        }

        inv.setItem(49, createItem(Material.BARRIER, "§c閉じる"));
        inv.setItem(53, createItem(Material.ARROW, "§7メインメニュー"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getSlot();

        switch (slot) {
            case 10 -> player.performCommand("party info");
            case 11 -> {
                Party party = partyManager.getParty(player);
                if (party == null) break;
                if (party.isLeader(player.getUniqueId())) {
                    if (party.isPublic()) {
                        player.performCommand("party private");
                    } else {
                        player.performCommand("party public");
                    }
                }
            }
            case 12 -> open(player);
            case 13 -> {
                Party party = partyManager.getParty(player);
                if (party == null) {
                    player.performCommand("party create");
                } else if (party.isLeader(player.getUniqueId())) {
                    player.performCommand("party disband");
                } else {
                    player.performCommand("party leave");
                }
            }
            case 47 -> {
                int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                if (page > 0) {
                    playerPage.put(player.getUniqueId(), page - 1);
                    open(player);
                }
            }
            case 51 -> {
                int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                int totalPages = Math.max(1, (int) Math.ceil(
                        (double) partyManager.getPublicParties().size() / PARTIES_PER_PAGE));
                if (page < totalPages - 1) {
                    playerPage.put(player.getUniqueId(), page + 1);
                    open(player);
                }
            }
            case 49 -> player.closeInventory();
            case 53 -> player.closeInventory();
            default -> {
                if (slot >= 19 && slot <= 25) {
                    handleJoinPublicParty(player, slot);
                }
            }
        }
    }

    private void handleJoinPublicParty(Player player, int slot) {
        if (partyManager.isInParty(player)) return;

        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int index = (page * PARTIES_PER_PAGE) + (slot - 19);
        List<Party> publicParties = partyManager.getPublicParties();
        if (index >= 0 && index < publicParties.size()) {
            Party target = publicParties.get(index);
            partyManager.joinPublicParty(player, target.getId());
        }
    }

    private ItemStack createPartyListingItem(Party party) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        Player leader = Bukkit.getPlayer(party.getLeaderId());
        if (leader != null) {
            meta.setOwningPlayer(leader);
        }

        meta.displayName(Component.text("§e" + getName(party.getLeaderId()) + " のパーティー"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7リーダー: " + getName(party.getLeaderId()))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§7メンバー: " + party.getSize() + "/" + party.getMaxMembers())
                .decoration(TextDecoration.ITALIC, false));
        if (!party.getTags().isEmpty()) {
            lore.add(Component.text("タグ: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.join(
                            JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
                            party.getTags().stream().map(PartyTag::getComponent).toList()
                    )));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("§aクリックして参加", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createPlayerHead(Player player, String name, String... loreLines) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
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

    private String getName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : "Unknown";
    }
}
