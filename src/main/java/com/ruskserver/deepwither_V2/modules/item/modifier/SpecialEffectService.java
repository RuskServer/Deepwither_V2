package com.ruskserver.deepwither_V2.modules.item.modifier;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

@Service
public class SpecialEffectService {

    private final ItemPDCUtil pdcUtil;

    @Inject
    public SpecialEffectService(ItemPDCUtil pdcUtil) {
        this.pdcUtil = pdcUtil;
    }

    public boolean hasEffect(LivingEntity entity, SpecialEffect target) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return false;

        if (hasEffect(equipment.getItemInMainHand(), target)
                || hasEffect(equipment.getItemInOffHand(), target)) {
            return true;
        }
        for (ItemStack item : equipment.getArmorContents()) {
            if (hasEffect(item, target)) return true;
        }
        return false;
    }

    private boolean hasEffect(ItemStack item, SpecialEffect target) {
        if (item == null || item.isEmpty()) return false;
        if (pdcUtil.isBroken(item)) return false;
        return pdcUtil.getSpecialEffects(item).stream()
                .anyMatch(effect -> effect.getEffect() == target);
    }
}
