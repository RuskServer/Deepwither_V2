package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.UUID;

@Component
public class MartyrdomListener implements Listener {

    private final MartyrdomSkill martyrdomSkill;
    private final VirtualHealthManager healthManager;

    @Inject
    public MartyrdomListener(MartyrdomSkill martyrdomSkill, VirtualHealthManager healthManager) {
        this.martyrdomSkill = martyrdomSkill;
        this.healthManager = healthManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player defender)) return;

        Map<UUID, Long> active = martyrdomSkill.getActiveMartyrdom();
        if (active.isEmpty()) return;

        for (Map.Entry<UUID, Long> entry : active.entrySet()) {
            UUID martyrId = entry.getKey();
            if (System.currentTimeMillis() > entry.getValue()) {
                active.remove(martyrId);
                continue;
            }

            if (martyrId.equals(defender.getUniqueId())) continue;

            Player martyr = org.bukkit.Bukkit.getPlayer(martyrId);
            if (martyr == null || !martyr.isOnline()) continue;

            if (martyr.getLocation().distanceSquared(defender.getLocation()) > 7.0 * 7.0) continue;

            double originalDamage = event.getDamage();

            double redirectedDamage = originalDamage * 0.5;

            if (redirectedDamage > 0) {
                healthManager.damage(martyr, redirectedDamage);
            }
        }
    }
}
