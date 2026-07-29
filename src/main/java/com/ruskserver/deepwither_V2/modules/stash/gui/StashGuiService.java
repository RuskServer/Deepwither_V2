package com.ruskserver.deepwither_V2.modules.stash.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.stash.StashService;
import com.ruskserver.deepwither_V2.modules.stash.StashService.MigrationResult;
import com.ruskserver.deepwither_V2.modules.stash.StashService.SaveResult;
import com.ruskserver.deepwither_V2.modules.stash.StashService.StashAccess;
import com.ruskserver.deepwither_V2.modules.stash.StashService.UpgradeResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StashGuiService implements Stoppable {

    private static final int GUI_SIZE = 36;
    private static final int CONTROL_ROW_START = 27;
    private static final int PREVIOUS_SLOT = 27;
    private static final int INFO_SLOT = 30;
    private static final int UPGRADE_SLOT = 32;
    private static final int NEXT_SLOT = 35;
    private static final int CONFIRM_SLOT = 11;
    private static final int CONFIRM_INFO_SLOT = 13;
    private static final int CANCEL_SLOT = 15;

    private final Deepwither_V2 plugin;
    private final StashService stashService;
    private final Map<UUID, StashSession> sessions = new HashMap<>();

    @Inject
    public StashGuiService(Deepwither_V2 plugin, StashService stashService) {
        this.plugin = plugin;
        this.stashService = stashService;
    }

    public void open(Player player) {
        StashAccess access = stashService.resolveAccess(player).orElse(null);
        if (access == null) {
            player.sendMessage(Component.text("キャラクターを選択してから使用してください。", NamedTextColor.RED));
            return;
        }

        MigrationResult migration = stashService.migrateNativeEnderChest(player, access);
        switch (migration.status()) {
            case NO_SPACE -> {
                player.sendMessage(Component.text("既存のエンダーチェスト内容を移行する空きがありません。", NamedTextColor.RED));
                return;
            }
            case SAVE_FAILED -> {
                player.sendMessage(Component.text("スタッシュの初期化に失敗しました。時間を置いて再度お試しください。", NamedTextColor.RED));
                return;
            }
            case MIGRATED -> player.sendMessage(Component.text(
                    "既存のエンダーチェスト内容をスタッシュへ移行しました。",
                    NamedTextColor.GREEN
            ));
            case NOT_REQUIRED, INITIALIZED -> {
            }
        }

        openPage(player, migration.access(), 0);
    }

    public void handleClick(StashInventoryHolder holder, InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !isCurrent(holder, player)) {
            event.setCancelled(true);
            return;
        }

        StashSession session = sessions.get(player.getUniqueId());
        if (holder.getViewType() == StashInventoryHolder.ViewType.UPGRADE_CONFIRM) {
            event.setCancelled(true);
            if (event.getRawSlot() == CONFIRM_SLOT) {
                purchaseUpgrade(player, session);
            } else if (event.getRawSlot() == CANCEL_SLOT) {
                openPage(player, session.access, Math.min(session.page, session.access.data().getUnlockedPages() - 1));
            }
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= CONTROL_ROW_START && rawSlot < GUI_SIZE) {
            event.setCancelled(true);
            if (event.getCursor() != null && !event.getCursor().isEmpty()) {
                player.sendMessage(Component.text("カーソル上のアイテムを置いてから操作してください。", NamedTextColor.RED));
                return;
            }
            switch (rawSlot) {
                case PREVIOUS_SLOT -> switchPage(player, session, session.page - 1);
                case NEXT_SLOT -> switchPage(player, session, session.page + 1);
                case UPGRADE_SLOT -> openUpgradeConfirm(player, session);
                default -> {
                }
            }
            return;
        }

        if (event.getAction() == InventoryAction.CLONE_STACK
                || event.getAction() == InventoryAction.UNKNOWN) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && rawSlot >= 0
                && rawSlot < StashService.PAGE_SIZE
                && isRestrictedWeapon(event.getCurrentItem())) {
            event.setCancelled(true);
            player.sendMessage(Component.text(
                    "武器は通常クリックで取り出してください。",
                    NamedTextColor.YELLOW
            ));
            return;
        }

        scheduleSave(player.getUniqueId(), holder.getSessionToken());
    }

    public void handleDrag(StashInventoryHolder holder, InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !isCurrent(holder, player)) {
            event.setCancelled(true);
            return;
        }
        if (holder.getViewType() == StashInventoryHolder.ViewType.UPGRADE_CONFIRM) {
            event.setCancelled(true);
            return;
        }

        boolean touchesControls = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= CONTROL_ROW_START && slot < GUI_SIZE);
        if (touchesControls) {
            event.setCancelled(true);
            return;
        }
        scheduleSave(player.getUniqueId(), holder.getSessionToken());
    }

    public void handleClose(StashInventoryHolder holder) {
        StashSession session = sessions.get(holder.getPlayerId());
        if (session == null || !holder.getSessionToken().equals(session.token)) {
            return;
        }
        if (holder.getViewType() == StashInventoryHolder.ViewType.STASH) {
            saveCurrent(session);
        }
        sessions.remove(holder.getPlayerId());
    }

    public void closePlayer(UUID playerId) {
        StashSession session = sessions.remove(playerId);
        if (session != null && session.viewType == StashInventoryHolder.ViewType.STASH) {
            saveCurrent(session);
        }
    }

    @Override
    public void stop() {
        for (StashSession session : List.copyOf(sessions.values())) {
            if (session.viewType == StashInventoryHolder.ViewType.STASH) {
                saveCurrent(session);
            }
        }
        sessions.clear();
    }

    private void switchPage(Player player, StashSession session, int targetPage) {
        if (targetPage < 0 || targetPage >= session.access.data().getUnlockedPages()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.2f);
            return;
        }
        if (!saveCurrent(session)) {
            player.sendMessage(Component.text("保存に失敗したためページを移動できません。", NamedTextColor.RED));
            return;
        }
        openPage(player, session.access, targetPage);
    }

    private void openUpgradeConfirm(Player player, StashSession session) {
        if (session.access.data().getUnlockedPages() >= StashService.MAX_PAGES) {
            player.sendMessage(Component.text("スタッシュは最大まで拡張されています。", NamedTextColor.YELLOW));
            return;
        }
        if (!saveCurrent(session)) {
            player.sendMessage(Component.text("保存に失敗したためアップグレードできません。", NamedTextColor.RED));
            return;
        }

        UUID token = UUID.randomUUID();
        StashInventoryHolder holder = new StashInventoryHolder(
                player.getUniqueId(),
                token,
                StashInventoryHolder.ViewType.UPGRADE_CONFIRM,
                session.page
        );
        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text("スタッシュ拡張の確認", NamedTextColor.DARK_PURPLE)
        );
        holder.setInventory(inventory);
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE);

        int currentPages = session.access.data().getUnlockedPages();
        double price = stashService.getNextUpgradePrice(session.access);
        inventory.setItem(CONFIRM_SLOT, button(
                Material.LIME_STAINED_GLASS_PANE,
                Component.text("購入する", NamedTextColor.GREEN),
                List.of(Component.text(stashService.formatMoney(price) + "を支払います", NamedTextColor.GRAY))
        ));
        inventory.setItem(CONFIRM_INFO_SLOT, button(
                Material.ENDER_CHEST,
                Component.text("容量を拡張", NamedTextColor.LIGHT_PURPLE),
                List.of(
                        Component.text(currentPages * StashService.PAGE_SIZE + "枠 → "
                                + (currentPages + 1) * StashService.PAGE_SIZE + "枠", NamedTextColor.WHITE),
                        Component.text("費用: " + stashService.formatMoney(price), NamedTextColor.GOLD),
                        Component.text("残高: " + stashService.formatMoney(stashService.getBalance(player)), NamedTextColor.YELLOW)
                )
        ));
        inventory.setItem(CANCEL_SLOT, button(
                Material.RED_STAINED_GLASS_PANE,
                Component.text("キャンセル", NamedTextColor.RED),
                List.of()
        ));

        session.token = token;
        session.inventory = inventory;
        session.viewType = StashInventoryHolder.ViewType.UPGRADE_CONFIRM;
        session.saveScheduled = false;
        player.openInventory(inventory);
    }

    private void purchaseUpgrade(Player player, StashSession session) {
        UpgradeResult result = stashService.purchaseNextPage(player, session.access);
        session.access = result.access();

        switch (result.status()) {
            case SUCCESS -> {
                player.sendMessage(Component.text(
                        "スタッシュを" + result.access().data().getUnlockedPages() + "ページへ拡張しました。残高: "
                                + stashService.formatMoney(result.balance()),
                        NamedTextColor.GREEN
                ));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
            }
            case MAX_LEVEL -> player.sendMessage(Component.text("スタッシュは最大まで拡張されています。", NamedTextColor.YELLOW));
            case IN_PROGRESS -> player.sendMessage(Component.text("アップグレード処理中です。", NamedTextColor.YELLOW));
            case ECONOMY_UNAVAILABLE -> player.sendMessage(Component.text("経済システムが利用できません。", NamedTextColor.RED));
            case INSUFFICIENT_FUNDS -> player.sendMessage(Component.text(
                    "所持金が不足しています。必要: " + stashService.formatMoney(result.price())
                            + " / 現在: " + stashService.formatMoney(result.balance()),
                    NamedTextColor.RED
            ));
            case WITHDRAW_FAILED -> player.sendMessage(Component.text("支払い処理に失敗しました。", NamedTextColor.RED));
            case SAVE_FAILED -> player.sendMessage(Component.text(
                    "容量の保存に失敗したため、支払額を返金しました。",
                    NamedTextColor.RED
            ));
            case REFUND_FAILED -> player.sendMessage(Component.text(
                    "容量保存と返金に失敗しました。管理者へ連絡してください。",
                    NamedTextColor.DARK_RED
            ));
        }

        openPage(player, session.access, Math.min(session.page, session.access.data().getUnlockedPages() - 1));
    }

    private void openPage(Player player, StashAccess access, int page) {
        int safePage = Math.max(0, Math.min(page, access.data().getUnlockedPages() - 1));
        UUID token = UUID.randomUUID();
        StashInventoryHolder holder = new StashInventoryHolder(
                player.getUniqueId(),
                token,
                StashInventoryHolder.ViewType.STASH,
                safePage
        );
        Inventory inventory = Bukkit.createInventory(
                holder,
                GUI_SIZE,
                Component.text("スタッシュ ", NamedTextColor.DARK_PURPLE)
                        .append(Component.text((safePage + 1) + "/" + access.data().getUnlockedPages(), NamedTextColor.WHITE))
        );
        holder.setInventory(inventory);

        ItemStack[] pageContents = stashService.loadPage(access, safePage);
        for (int slot = 0; slot < StashService.PAGE_SIZE; slot++) {
            inventory.setItem(slot, pageContents[slot]);
        }
        renderControls(inventory, access, safePage);

        StashSession session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new StashSession());
        session.token = token;
        session.access = access;
        session.page = safePage;
        session.inventory = inventory;
        session.viewType = StashInventoryHolder.ViewType.STASH;
        session.saveScheduled = false;

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.0f);
    }

    private void renderControls(Inventory inventory, StashAccess access, int page) {
        for (int slot = CONTROL_ROW_START; slot < GUI_SIZE; slot++) {
            inventory.setItem(slot, button(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), List.of()));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, button(
                    Material.ARROW,
                    Component.text("前のページ", NamedTextColor.YELLOW),
                    List.of()
            ));
        }
        if (page + 1 < access.data().getUnlockedPages()) {
            inventory.setItem(NEXT_SLOT, button(
                    Material.ARROW,
                    Component.text("次のページ", NamedTextColor.YELLOW),
                    List.of()
            ));
        }

        inventory.setItem(INFO_SLOT, button(
                Material.CHEST,
                Component.text("ページ " + (page + 1) + "/" + access.data().getUnlockedPages(), NamedTextColor.AQUA),
                List.of(Component.text(
                        "解放済み: " + access.data().getUnlockedPages() * StashService.PAGE_SIZE + "枠",
                        NamedTextColor.GRAY
                ))
        ));

        if (access.data().getUnlockedPages() < StashService.MAX_PAGES) {
            inventory.setItem(UPGRADE_SLOT, button(
                    Material.EMERALD,
                    Component.text("スタッシュを拡張", NamedTextColor.GREEN),
                    List.of(Component.text(
                            "費用: " + stashService.formatMoney(stashService.getNextUpgradePrice(access)),
                            NamedTextColor.GOLD
                    ))
            ));
        } else {
            inventory.setItem(UPGRADE_SLOT, button(
                    Material.NETHER_STAR,
                    Component.text("最大容量", NamedTextColor.LIGHT_PURPLE),
                    List.of()
            ));
        }
    }

    private boolean saveCurrent(StashSession session) {
        if (session.inventory == null || session.viewType != StashInventoryHolder.ViewType.STASH) {
            return true;
        }
        SaveResult result = stashService.savePage(session.access, session.page, session.inventory);
        session.access = result.access();
        return result.success();
    }

    private void scheduleSave(UUID playerId, UUID token) {
        StashSession session = sessions.get(playerId);
        if (session == null || session.saveScheduled) {
            return;
        }
        session.saveScheduled = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            StashSession current = sessions.get(playerId);
            if (current == null) {
                return;
            }
            current.saveScheduled = false;
            if (!token.equals(current.token)
                    || current.viewType != StashInventoryHolder.ViewType.STASH) {
                return;
            }
            if (!saveCurrent(current)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.sendMessage(Component.text(
                            "スタッシュの保存に失敗しました。内容を保持したまま再試行します。",
                            NamedTextColor.RED
                    ));
                }
            }
        });
    }

    private boolean isCurrent(StashInventoryHolder holder, Player player) {
        StashSession session = sessions.get(player.getUniqueId());
        return holder.getPlayerId().equals(player.getUniqueId())
                && session != null
                && holder.getSessionToken().equals(session.token);
    }

    private void fill(Inventory inventory, Material material) {
        ItemStack filler = button(material, Component.empty(), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack button(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream()
                    .map(line -> line.decoration(TextDecoration.ITALIC, false))
                    .toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private boolean isRestrictedWeapon(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        if (item.getType() == Material.STICK && item.lore() != null) {
            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
            boolean scroll = item.lore().stream()
                    .anyMatch(line -> serializer.serialize(line).contains("カテゴリ:スクロール"));
            if (scroll) {
                return false;
            }
        }

        Material material = item.getType();
        return Tag.ITEMS_SWORDS.isTagged(material)
                || Tag.ITEMS_AXES.isTagged(material)
                || material == Material.BOW
                || material == Material.MACE
                || material == Material.TRIDENT
                || material == Material.CROSSBOW
                || material == Material.STICK
                || material == Material.FEATHER;
    }

    private static final class StashSession {
        private UUID token;
        private StashAccess access;
        private int page;
        private Inventory inventory;
        private StashInventoryHolder.ViewType viewType;
        private boolean saveScheduled;
    }
}
