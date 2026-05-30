package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class PlayerInventoryRestrictor implements Listener {

    private static final int HOTBAR_HEAD = 0;
    private static final int HOTBAR_TAIL = 8;
    private static final int STORAGE_HEAD = 9;
    private static final int STORAGE_TAIL = 35;
    private static final int OFFHAND_SLOT = 40;

    private static final List<Strategy> WEAPON = List.of(Strategy.MERGE_MAINHAND, Strategy.MERGE_OFFHAND, Strategy.MERGE_STORAGE, Strategy.PLACE_STORAGE);
    private static final List<Strategy> REVERSE = List.of(Strategy.MERGE_MAINHAND, Strategy.MERGE_OFFHAND, Strategy.MERGE_STORAGE, Strategy.MERGE_HOTBAR, Strategy.PLACE_STORAGE, Strategy.PLACE_HOTBAR);
    private static final List<Strategy> QUICKMOVE = List.of(Strategy.MERGE_HOTBAR, Strategy.PLACE_HOTBAR);
    private static final List<Strategy> CANCEL = List.of();
    private static final List<Strategy> VANILLA = List.of(Strategy.MERGE_MAINHAND, Strategy.MERGE_OFFHAND, Strategy.MERGE_HOTBAR, Strategy.MERGE_STORAGE, Strategy.PLACE_HOTBAR, Strategy.PLACE_STORAGE);

    private static final Component MULTIPLE_WEAPONS = Component.text("ホットバーに武器は1つしか置けません。", NamedTextColor.RED);
    private static final Component OVERFLOW_WEAPONS = Component.text("これ以上持てない！", NamedTextColor.RED);

    private final ItemPDCUtil pdcUtil;

    @Inject
    public PlayerInventoryRestrictor(ItemPDCUtil pdcUtil) {
        this.pdcUtil = pdcUtil;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        switch (event.getAction()) {
            case PLACE_ALL, PLACE_ONE -> {
                int clickedSlot = event.getSlot();

                if (event.getClickedInventory() instanceof PlayerInventory
                    && HOTBAR_HEAD <= clickedSlot && clickedSlot <= HOTBAR_TAIL
                    && isWeapon(event.getCursor())
                    && !isWeapon(event.getCurrentItem())
                    && hasWeaponInHotbar(player.getInventory())
                ) {
                    player.sendMessage(MULTIPLE_WEAPONS);
                    event.setCancelled(true);
                }
            }
            case MOVE_TO_OTHER_INVENTORY -> {
                ItemStack source = event.getCurrentItem();
                if (source == null) return;

                if (event.getSlotType() == InventoryType.SlotType.RESULT && isWeapon(source)) {
                    event.setCancelled(true);
                    return;
                }

                Inventory clicked = event.getClickedInventory();
                if (clicked == null) return;

                PlayerInventory inventory = player.getInventory();

                if (clicked.getType() == InventoryType.PLAYER && event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
                    int slot = event.getSlot();

                    if (slot < HOTBAR_HEAD || HOTBAR_TAIL < slot) {
                        boolean isWeapon = isWeapon(source);
                        boolean hasWeaponInHotbar = hasWeaponInHotbar(inventory);

                        applyStrategy(isWeapon && hasWeaponInHotbar ? CANCEL : QUICKMOVE, inventory, source);

                        if (isWeapon && !source.isEmpty() && hasWeaponInHotbar) {
                            player.sendMessage(MULTIPLE_WEAPONS);
                        }

                        event.setCancelled(true);
                    }
                }
            }
            case HOTBAR_SWAP -> {
                int slot = event.getSlot();
                PlayerInventory inventory = player.getInventory();
                int button = event.getHotbarButton();
                if (button == -1) {
                    if (event.getClickedInventory() instanceof PlayerInventory && HOTBAR_HEAD <= slot && slot <= HOTBAR_TAIL
                        && isWeapon(inventory.getItemInOffHand())
                        && !isWeapon(inventory.getItemInMainHand())
                        && hasWeaponInHotbar(inventory)
                    ) {
                        player.sendMessage(MULTIPLE_WEAPONS);
                        event.setCancelled(true);
                    }
                } else if (event.getClickedInventory() instanceof PlayerInventory && slot != button && hasWeaponInHotbar(inventory)
                    && (HOTBAR_HEAD <= slot && slot <= HOTBAR_TAIL && isWeapon(inventory.getItem(button)) && !isWeapon(inventory.getItem(slot))
                    || STORAGE_HEAD <= slot && slot <= STORAGE_TAIL && !isWeapon(inventory.getItem(button)) && isWeapon(inventory.getItem(slot)))
                ) {
                    player.sendMessage(MULTIPLE_WEAPONS);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwap(@NotNull PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (isWeapon(event.getMainHandItem()) && hasWeaponInHotbar(player.getInventory()) && !isWeapon(event.getOffHandItem())) {
            player.sendMessage(MULTIPLE_WEAPONS);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && isWeapon(event.getOldCursor()) && hasWeaponInHotbar(player.getInventory())) {
            var view = event.getView();

            var rawSlots = new HashSet<>(event.getRawSlots());
            if (rawSlots.removeIf(raw -> {
                var inv = view.getInventory(raw);

                if (inv != null && inv.getType() == InventoryType.PLAYER) {
                    int slot = view.convertSlot(raw);
                    return HOTBAR_HEAD <= slot && slot <= HOTBAR_TAIL;
                }

                return false;
            })) {
                player.sendMessage(MULTIPLE_WEAPONS);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        var view = event.getView();
        ItemStack source = view.getCursor();
        PlayerInventory inventory = player.getInventory();

        applyStrategy(isWeapon(source) ? hasWeaponInHotbar(inventory) ? WEAPON : VANILLA : REVERSE, inventory, source);

        if (!source.isEmpty()) {
            player.sendMessage(OVERFLOW_WEAPONS);
            player.getWorld().dropItemNaturally(player.getLocation(), source);
            view.setCursor(null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        event.setCancelled(true);

        ItemStack source = event.getItem().getItemStack().clone();
        int amount = source.getAmount();
        PlayerInventory inventory = player.getInventory();

        boolean isWeapon = isWeapon(source);
        applyStrategy(isWeapon ? hasWeaponInHotbar(inventory) ? WEAPON : VANILLA : REVERSE, inventory, source);

        int placed = amount - source.getAmount();
        if (placed > 0) {
            player.sendActionBar(Component.text()
                .append(Component.text("+ ", NamedTextColor.GRAY))
                .append(Optional.ofNullable(source.getItemMeta())
                    .map(ItemMeta::displayName)
                    .orElse(Component.translatable(source)))
                .append(Component.text(" x" + placed, NamedTextColor.WHITE)));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, 1.0f);

            if (source.isEmpty()) {
                event.getItem().remove();
            } else {
                event.getItem().getItemStack().setAmount(source.getAmount());
            }
        }
    }

    private boolean isWeapon(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;

        if (itemStack.getType() == Material.STICK) {
            var lore = itemStack.lore();
            if (lore != null) {
                var serializer = PlainTextComponentSerializer.plainText();
                for (var line : lore) {
                    if (serializer.serialize(line).contains("カテゴリ:スクロール")) {
                        return false;
                    }
                }
            }
        }

        Material material = itemStack.getType();

        return Tag.ITEMS_SWORDS.isTagged(material)
            || Tag.ITEMS_AXES.isTagged(material)
            || material == Material.BOW
            || material == Material.MACE
            || material == Material.TRIDENT
            || material == Material.CROSSBOW
            || material == Material.STICK
            || material == Material.FEATHER;
    }

    private boolean hasWeaponInHotbar(PlayerInventory inventory) {
        for (int i = HOTBAR_HEAD; i <= HOTBAR_TAIL; i++) {
            if (isWeapon(inventory.getItem(i))) return true;
        }
        return false;
    }

    private enum Strategy {
        MERGE_MAINHAND, MERGE_OFFHAND, MERGE_HOTBAR, MERGE_STORAGE, PLACE_HOTBAR, PLACE_STORAGE
    }

    private static void applyStrategy(List<Strategy> strategies, PlayerInventory inventory, ItemStack source) {
        if (source == null || source.isEmpty()) return;

        for (var s : strategies) {
            if (source.isEmpty()) break;
            switch (s) {
                case MERGE_MAINHAND -> mergeSlot(inventory, inventory.getHeldItemSlot(), source);
                case MERGE_OFFHAND -> mergeSlot(inventory, OFFHAND_SLOT, source);
                case MERGE_HOTBAR -> {
                    for (int i = HOTBAR_HEAD; i <= HOTBAR_TAIL; i++) {
                        if (source.isEmpty()) break;
                        mergeSlot(inventory, i, source);
                    }
                }
                case MERGE_STORAGE -> {
                    for (int i = STORAGE_HEAD; i <= STORAGE_TAIL; i++) {
                        if (source.isEmpty()) break;
                        mergeSlot(inventory, i, source);
                    }
                }
                case PLACE_HOTBAR -> {
                    for (int i = HOTBAR_HEAD; i <= HOTBAR_TAIL; i++) {
                        if (source.isEmpty()) break;
                        placeSlot(inventory, i, source);
                    }
                }
                case PLACE_STORAGE -> {
                    for (int i = STORAGE_HEAD; i <= STORAGE_TAIL; i++) {
                        if (source.isEmpty()) break;
                        placeSlot(inventory, i, source);
                    }
                }
            }
        }
    }

    private static void mergeSlot(Inventory inventory, int slot, ItemStack source) {
        if (source.isEmpty()) return;
        ItemStack target = inventory.getItem(slot);
        if (target == null || target.isEmpty() || !target.isSimilar(source)) return;
        int space = target.getMaxStackSize() - target.getAmount();
        if (space <= 0) return;
        int add = Math.min(space, source.getAmount());
        target.setAmount(target.getAmount() + add);
        source.setAmount(source.getAmount() - add);
        inventory.setItem(slot, target);
    }

    private static void placeSlot(Inventory inventory, int slot, ItemStack source) {
        if (source.isEmpty()) return;
        ItemStack current = inventory.getItem(slot);
        if (current != null && !current.isEmpty()) return;
        inventory.setItem(slot, source.clone());
        source.setAmount(0);
    }
}
