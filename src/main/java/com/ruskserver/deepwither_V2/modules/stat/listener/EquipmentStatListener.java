package com.ruskserver.deepwither_V2.modules.stat.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 所持装備（メインハンド武器 + 防具4部位）のステータスをStatManagerに反映させるリスナー。
 */
@Component
public class EquipmentStatListener implements Listener {

    private static final String[] ARMOR_SOURCE_BASE = {"equip_armor_head_base", "equip_armor_chest_base", "equip_armor_legs_base", "equip_armor_boots_base"};
    private static final String[] ARMOR_SOURCE_MOD  = {"equip_armor_head_mod",  "equip_armor_chest_mod",  "equip_armor_legs_mod",  "equip_armor_boots_mod"};

    private final StatManager statManager;
    private final ItemPDCUtil pdcUtil;
    private final ItemManager itemManager;
    private final Deepwither_V2 plugin;

    @Inject
    public EquipmentStatListener(StatManager statManager, ItemPDCUtil pdcUtil, ItemManager itemManager, Deepwither_V2 plugin) {
        this.statManager = statManager;
        this.pdcUtil = pdcUtil;
        this.itemManager = itemManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateEquipmentStats(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        statManager.removeProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateEquipmentStats(event.getPlayer());
        }, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            updateEquipmentStats(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // プレイヤーインベントリ内の防具スロット（5〜8）または自分以外のインベントリからの移動を検出
        int slot = event.getRawSlot();
        if (slot >= 5 && slot <= 8) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateEquipmentStats(player), 1L);
        }
    }

    public void updateEquipmentStats(Player player) {
        removeAllEquipmentModifiers(player);
        applyMainHand(player, player.getInventory().getItemInMainHand());
        applyArmor(player, player.getInventory().getArmorContents());
    }

    private void removeAllEquipmentModifiers(Player player) {
        for (StatType type : StatType.values()) {
            statManager.removeModifier(player.getUniqueId(), type, "equip_mainhand_base");
            statManager.removeModifier(player.getUniqueId(), type, "equip_mainhand_mod");
            for (int i = 0; i < 4; i++) {
                statManager.removeModifier(player.getUniqueId(), type, ARMOR_SOURCE_BASE[i]);
                statManager.removeModifier(player.getUniqueId(), type, ARMOR_SOURCE_MOD[i]);
            }
        }
    }

    private void applyMainHand(Player player, ItemStack item) {
        if (item == null || item.isEmpty() || item.getType() == Material.AIR) return;
        applyItemStats(player, item, "equip_mainhand_base", "equip_mainhand_mod");
    }

    private void applyArmor(Player player, ItemStack[] armorContents) {
        if (armorContents == null) return;
        for (int i = 0; i < Math.min(armorContents.length, 4); i++) {
            ItemStack piece = armorContents[i];
            if (piece == null || piece.isEmpty() || piece.getType() == Material.AIR) continue;
            applyItemStats(player, piece, ARMOR_SOURCE_BASE[i], ARMOR_SOURCE_MOD[i]);
        }
    }

    private void applyItemStats(Player player, ItemStack item, String baseSourceId, String modSourceId) {
        String customId = pdcUtil.getItemId(item);
        if (customId == null) return;

        CustomItem customItem = itemManager.getCustomItem(customId);
        if (customItem != null) {
            for (Map.Entry<StatType, Double> entry : customItem.getBaseStats().entrySet()) {
                statManager.setModifier(player.getUniqueId(), entry.getKey(), baseSourceId, entry.getValue(), ModifierType.ADDITIVE);
            }
        }

        Map<StatType, Double> modifiers = pdcUtil.getModifiers(item);
        for (Map.Entry<StatType, Double> entry : modifiers.entrySet()) {
            statManager.setModifier(player.getUniqueId(), entry.getKey(), modSourceId, entry.getValue(), ModifierType.ADDITIVE);
        }
    }
}
