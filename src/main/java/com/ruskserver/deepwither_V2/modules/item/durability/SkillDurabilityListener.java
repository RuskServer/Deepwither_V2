package com.ruskserver.deepwither_V2.modules.item.durability;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillCastCompleteEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillExecuteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@Component
public class SkillDurabilityListener implements Listener {

    private final EquipmentDurabilityService durabilityService;

    @Inject
    public SkillDurabilityListener(EquipmentDurabilityService durabilityService) {
        this.durabilityService = durabilityService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSkillExecute(SkillExecuteEvent event) {
        if (!event.getSkill().getRoles().contains(SkillTag.Role.ATTACK)) return;
        ItemStack hand = event.getCaster().getInventory().getItemInMainHand();
        if (durabilityService.isDurable(hand)
                && !durabilityService.canUse(event.getCaster(), hand, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSkillComplete(SkillCastCompleteEvent event) {
        if (!event.getSkill().getRoles().contains(SkillTag.Role.ATTACK)) return;
        ItemStack hand = event.getCaster().getInventory().getItemInMainHand();
        if (durabilityService.isDurable(hand)) {
            durabilityService.damageItem(event.getCaster(), hand, 1);
        }
    }
}
