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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 手に持っているアイテムのステータスをStatManagerに反映させるリスナー。
 * （※防具の反映には別途ArmorEquipEventのようなカスタムイベントの実装が必要ですが、
 * 今回はメインハンドの武器反映のみをテスト実装しています）
 */
@Component
public class EquipmentStatListener implements Listener {

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
        updateMainHandStat(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        statManager.removeProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        // イベント中はまだインベントリの中身が切り替わっていないため、1tick遅らせて更新
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateMainHandStat(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand());
        }, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            updateMainHandStat((Player) event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand());
        }
    }

    private void updateMainHandStat(Player player, ItemStack mainHandItem) {
        // 一旦、過去のメインハンド武器のステータスをリセットする
        for (StatType type : StatType.values()) {
            statManager.removeModifier(player.getUniqueId(), type, "equip_mainhand_base");
            statManager.removeModifier(player.getUniqueId(), type, "equip_mainhand_mod");
        }

        if (mainHandItem == null || mainHandItem.isEmpty()) {
            return;
        }

        // PDCからカスタムアイテムかどうかを判定
        String customId = pdcUtil.getItemId(mainHandItem);
        if (customId == null) {
            return; // バニラアイテム
        }

        CustomItem customItem = itemManager.getCustomItem(customId);
        if (customItem != null) {
            // ベースステータスを加算
            for (Map.Entry<StatType, Double> entry : customItem.getBaseStats().entrySet()) {
                statManager.setModifier(player.getUniqueId(), entry.getKey(), "equip_mainhand_base", entry.getValue(), ModifierType.ADDITIVE);
            }
        }

        // ランダムモディファイアの値を加算
        Map<StatType, Double> modifiers = pdcUtil.getModifiers(mainHandItem);
        for (Map.Entry<StatType, Double> entry : modifiers.entrySet()) {
            statManager.setModifier(player.getUniqueId(), entry.getKey(), "equip_mainhand_mod", entry.getValue(), ModifierType.ADDITIVE);
        }
    }
}
