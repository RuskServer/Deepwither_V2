package com.ruskserver.deepwither_V2.modules.combat.damage.phases;

import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.item.set.EquipmentSetService;

public class EquipmentSetPhase implements DamagePhase {

    private final EquipmentSetService equipmentSetService;

    public EquipmentSetPhase(EquipmentSetService equipmentSetService) {
        this.equipmentSetService = equipmentSetService;
    }

    @Override
    public void process(DamageContext context) {
        equipmentSetService.processDefenderEffects(context);
    }
}
