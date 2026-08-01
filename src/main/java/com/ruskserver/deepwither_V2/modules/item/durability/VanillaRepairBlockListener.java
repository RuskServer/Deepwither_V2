package com.ruskserver.deepwither_V2.modules.item.durability;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;

@Component
public class VanillaRepairBlockListener implements Listener {

    private final EquipmentDurabilityService durabilityService;

    @Inject
    public VanillaRepairBlockListener(EquipmentDurabilityService durabilityService) {
        this.durabilityService = durabilityService;
    }

    @EventHandler
    public void onVanillaItemDamage(PlayerItemDamageEvent event) {
        if (durabilityService.isDurable(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {
        if (durabilityService.isDurable(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (containsDurableItem(event.getInventory().getContents())) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (containsDurableItem(event.getInventory().getContents())) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.isRepair() && containsDurableItem(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    private boolean containsDurableItem(ItemStack[] items) {
        for (ItemStack item : items) {
            if (durabilityService.isDurable(item)) return true;
        }
        return false;
    }
}
