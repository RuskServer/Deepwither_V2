package com.ruskserver.deepwither_V2.modules.item.durability;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.stat.listener.EquipmentStatListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EquipmentDurabilityService implements Listener {

    private static final long ARMOR_WEAR_INTERVAL_MILLIS = 500L;
    private static final long BROKEN_NOTICE_INTERVAL_MILLIS = 1_000L;

    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;
    private final EquipmentStatListener equipmentStatListener;
    private final Map<UUID, Long> lastArmorWear = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBrokenNotice = new ConcurrentHashMap<>();

    @Inject
    public EquipmentDurabilityService(ItemManager itemManager, ItemPDCUtil pdcUtil,
                                      EquipmentStatListener equipmentStatListener) {
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
        this.equipmentStatListener = equipmentStatListener;
    }

    public boolean isDurable(ItemStack item) {
        CustomItem customItem = resolve(item);
        return customItem != null && ItemDurabilityPolicy.getMaxDurability(customItem) > 0;
    }

    public boolean isBroken(ItemStack item) {
        return isDurable(item) && pdcUtil.isBroken(item);
    }

    public boolean canUse(Player player, ItemStack item, boolean notify) {
        if (!isBroken(item)) return true;
        if (notify) notifyBroken(player);
        return false;
    }

    public boolean damageItem(Player owner, ItemStack item, int amount) {
        CustomItem customItem = resolve(item);
        if (customItem == null || amount <= 0 || pdcUtil.isBroken(item)) return false;
        int maxDurability = ItemDurabilityPolicy.getMaxDurability(customItem);
        if (maxDurability <= 0 || !(item.getItemMeta() instanceof Damageable damageable)) return false;

        int nextDamage = damageable.getDamage() + amount;
        boolean broke = nextDamage >= maxDurability;
        damageable.setDamage(broke ? maxDurability - 1 : nextDamage);
        item.setItemMeta(damageable);
        if (broke) {
            pdcUtil.setBroken(item, true);
        }
        itemManager.updateItemMeta(item);

        if (broke && owner != null) {
            playBreakFeedback(owner);
            equipmentStatListener.updateEquipmentStats(owner);
        }
        return broke;
    }

    public void damageArmor(Player player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        if (now - lastArmorWear.getOrDefault(playerId, 0L) < ARMOR_WEAR_INTERVAL_MILLIS) return;
        lastArmorWear.put(playerId, now);

        boolean broke = false;
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack armor : armorContents) {
            broke |= damageItemWithoutStatRefresh(armor, 1);
        }
        player.getInventory().setArmorContents(armorContents);
        if (broke) {
            playBreakFeedback(player);
            equipmentStatListener.updateEquipmentStats(player);
        }
    }

    public boolean repairFully(Player player, ItemStack item) {
        CustomItem customItem = resolve(item);
        if (customItem == null || !(item.getItemMeta() instanceof Damageable damageable)) return false;
        if (ItemDurabilityPolicy.getMaxDurability(customItem) <= 0) return false;
        if (damageable.getDamage() <= 0 && !pdcUtil.isBroken(item)) return false;

        damageable.setDamage(0);
        item.setItemMeta(damageable);
        pdcUtil.setBroken(item, false);
        itemManager.updateItemMeta(item);
        equipmentStatListener.updateEquipmentStats(player);
        return true;
    }

    public int getMaxDurability(ItemStack item) {
        CustomItem customItem = resolve(item);
        return customItem == null ? 0 : ItemDurabilityPolicy.getMaxDurability(customItem);
    }

    public int getDamage(ItemStack item) {
        if (!(item != null && item.getItemMeta() instanceof Damageable damageable)) return 0;
        return Math.max(0, damageable.getDamage());
    }

    public UUID getOrCreateInstanceId(ItemStack item) {
        return isDurable(item) ? pdcUtil.ensureItemInstanceId(item) : null;
    }

    public ItemStack findByInstanceId(Player player, UUID instanceId) {
        if (instanceId == null) return null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && instanceId.equals(pdcUtil.getItemInstanceId(item))) {
                return item;
            }
        }
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastArmorWear.remove(playerId);
        lastBrokenNotice.remove(playerId);
    }

    private boolean damageItemWithoutStatRefresh(ItemStack item, int amount) {
        CustomItem customItem = resolve(item);
        if (customItem == null || amount <= 0 || pdcUtil.isBroken(item)) return false;
        int maxDurability = ItemDurabilityPolicy.getMaxDurability(customItem);
        if (maxDurability <= 0 || !(item.getItemMeta() instanceof Damageable damageable)) return false;
        int nextDamage = damageable.getDamage() + amount;
        boolean broke = nextDamage >= maxDurability;
        damageable.setDamage(broke ? maxDurability - 1 : nextDamage);
        item.setItemMeta(damageable);
        if (broke) pdcUtil.setBroken(item, true);
        itemManager.updateItemMeta(item);
        return broke;
    }

    private CustomItem resolve(ItemStack item) {
        String itemId = pdcUtil.getItemId(item);
        return itemId == null ? null : itemManager.getCustomItem(itemId);
    }

    private void notifyBroken(Player player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        if (now - lastBrokenNotice.getOrDefault(playerId, 0L) < BROKEN_NOTICE_INTERVAL_MILLIS) return;
        lastBrokenNotice.put(playerId, now);
        player.sendActionBar(Component.text("この装備は破損しています。合成屋で修理してください。",
                NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 0.8f);
    }

    private void playBreakFeedback(Player player) {
        player.sendMessage(Component.text("装備が破損しました。合成屋で修理できます。", NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.75f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.0, 0),
                12, 0.35, 0.45, 0.35, 0.02);
    }
}
