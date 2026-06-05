package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.health.event.VirtualHealthChangeEvent;
import com.ruskserver.deepwither_V2.modules.item.modifier.SpecialEffect;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SpecialEffectListener implements Listener {

    private static final long FORTIFY_COOLDOWN_MS = 5000;
    private static final long FORTIFY_DURATION_TICKS = 60;
    private static final double FORTIFY_DEFENSE_BOOST = 0.20;
    private static final double FORTIFY_PROC_CHANCE = 0.10;
    private static final String FORTIFY_SOURCE = "sp_fortify";

    private final ItemPDCUtil pdcUtil;
    private final StatManager statManager;
    private final Deepwither_V2 plugin;
    private final Map<UUID, Long> fortifyCooldowns = new HashMap<>();

    @Inject
    public SpecialEffectListener(ItemPDCUtil pdcUtil, StatManager statManager, Deepwither_V2 plugin) {
        this.pdcUtil = pdcUtil;
        this.statManager = statManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(VirtualHealthChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getNewHealth() >= event.getOldHealth()) return;

        if (!hasSpecialEffect(player, SpecialEffect.FORTIFY)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastProc = fortifyCooldowns.get(uuid);
        if (lastProc != null && (now - lastProc) < FORTIFY_COOLDOWN_MS) return;

        if (Math.random() >= FORTIFY_PROC_CHANCE) return;

        fortifyCooldowns.put(uuid, now);

        statManager.setModifier(uuid, StatType.DEFENSE, FORTIFY_SOURCE, FORTIFY_DEFENSE_BOOST, ModifierType.MULTIPLICATIVE);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            statManager.removeModifier(uuid, StatType.DEFENSE, FORTIFY_SOURCE);
        }, FORTIFY_DURATION_TICKS);
    }

    private boolean hasSpecialEffect(Player player, SpecialEffect target) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (hasEffect(item, target)) return true;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return hasEffect(mainHand, target);
    }

    private boolean hasEffect(ItemStack item, SpecialEffect target) {
        if (item == null || item.isEmpty()) return false;
        return pdcUtil.getSpecialEffects(item).stream()
                .anyMatch(e -> e.getEffect() == target);
    }
}
