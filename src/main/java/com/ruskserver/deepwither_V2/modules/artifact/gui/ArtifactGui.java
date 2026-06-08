package com.ruskserver.deepwither_V2.modules.artifact.gui;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactPDCUtil;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactSaveData;
import com.ruskserver.deepwither_V2.modules.artifact.service.ArtifactEquipmentService;
import com.ruskserver.deepwither_V2.modules.artifact.service.ArtifactStatService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ArtifactGui implements Listener {

    private static final String GUI_TITLE = "アーティファクト装備";
    private static final int GUI_SIZE = 27;

    // GUI内のアーティファクトスロットのインデックス
    private static final int[] ARTIFACT_SLOTS = {11, 13, 15};

    private final ArtifactEquipmentService equipmentService;
    private final ArtifactPDCUtil pdcUtil;
    private final ArtifactStatService statService;

    // 開いているプレイヤーのUUIDと、スロットの対応マップ
    private final Map<UUID, Inventory> openGuis = new HashMap<>();

    @Inject
    public ArtifactGui(ArtifactEquipmentService equipmentService, ArtifactPDCUtil pdcUtil, ArtifactStatService statService) {
        this.equipmentService = equipmentService;
        this.pdcUtil = pdcUtil;
        this.statService = statService;
    }

    public void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, Component.text(GUI_TITLE));

        // 背景を埋める
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.displayName(Component.empty());
        bg.setItemMeta(bgMeta);

        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, bg);
        }

        // プレイヤーの装備状況をロードして配置
        ArtifactSaveData artifactData = equipmentService.getEquippedArtifacts(player).orElse(null);
        if (artifactData != null) {
            for (int i = 0; i < ARTIFACT_SLOTS.length; i++) {
                int slotIndex = ARTIFACT_SLOTS[i];
                String base64 = artifactData.getEquippedArtifacts().get(i);
                if (base64 != null && !base64.isEmpty()) {
                    try {
                        byte[] bytes = Base64.getDecoder().decode(base64);
                        ItemStack artifact = ItemStack.deserializeBytes(bytes);
                        inv.setItem(slotIndex, artifact);
                    } catch (Exception e) {
                        inv.setItem(slotIndex, createEmptySlotIcon(i));
                    }
                } else {
                    inv.setItem(slotIndex, createEmptySlotIcon(i));
                }
            }
        } else {
            for (int i = 0; i < ARTIFACT_SLOTS.length; i++) {
                inv.setItem(ARTIFACT_SLOTS[i], createEmptySlotIcon(i));
            }
        }

        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), inv);
    }

    private ItemStack createEmptySlotIcon(int index) {
        ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("スロット " + (index + 1), NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openGuis.containsKey(player.getUniqueId())) return;
        if (!event.getView().title().equals(Component.text(GUI_TITLE))) return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        event.setCancelled(true);

        if (clickedInv.equals(event.getView().getTopInventory())) {
            // 上のインベントリ（GUI）をクリックした場合
            int slot = event.getSlot();
            int artifactIndex = getArtifactIndex(slot);

            if (artifactIndex != -1) {
                ItemStack currentItem = clickedInv.getItem(slot);
                if (currentItem != null && pdcUtil.isArtifact(currentItem)) {
                    // 脱着処理
                    HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(currentItem);
                    if (!leftOvers.isEmpty()) {
                        player.sendMessage(Component.text("インベントリが一杯です！", NamedTextColor.RED));
                        return;
                    }
                    clickedInv.setItem(slot, createEmptySlotIcon(artifactIndex));
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                    saveArtifacts(player, event.getView().getTopInventory());
                }
            }
        } else if (clickedInv.equals(event.getView().getBottomInventory())) {
            // 下のインベントリ（プレイヤーインベントリ）をクリックした場合
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && pdcUtil.isArtifact(clickedItem)) {
                // 空いているスロットを探して装備
                Inventory topInv = event.getView().getTopInventory();
                for (int i = 0; i < ARTIFACT_SLOTS.length; i++) {
                    int guiSlot = ARTIFACT_SLOTS[i];
                    ItemStack currentSlotItem = topInv.getItem(guiSlot);
                    if (currentSlotItem == null || !pdcUtil.isArtifact(currentSlotItem)) {
                        topInv.setItem(guiSlot, clickedItem.clone());
                        clickedInv.setItem(event.getSlot(), null);
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.0f);
                        saveArtifacts(player, topInv);
                        return;
                    }
                }
                player.sendMessage(Component.text("アーティファクトスロットが一杯です！", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (openGuis.remove(event.getPlayer().getUniqueId()) != null) {
            saveArtifacts((Player) event.getPlayer(), event.getInventory());
        }
    }

    private void saveArtifacts(Player player, Inventory guiInv) {
        ArtifactSaveData artifactData = new ArtifactSaveData();

        for (int i = 0; i < ARTIFACT_SLOTS.length; i++) {
            ItemStack item = guiInv.getItem(ARTIFACT_SLOTS[i]);
            if (item != null && pdcUtil.isArtifact(item)) {
                byte[] bytes = item.serializeAsBytes();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                artifactData.setEquippedArtifact(i, base64);
            } else {
                artifactData.setEquippedArtifact(i, null);
            }
        }

        if (equipmentService.saveEquippedArtifacts(player, artifactData)) {
            statService.applyArtifactStats(player);
        }
    }

    private int getArtifactIndex(int slot) {
        for (int i = 0; i < ARTIFACT_SLOTS.length; i++) {
            if (ARTIFACT_SLOTS[i] == slot) return i;
        }
        return -1;
    }
}
