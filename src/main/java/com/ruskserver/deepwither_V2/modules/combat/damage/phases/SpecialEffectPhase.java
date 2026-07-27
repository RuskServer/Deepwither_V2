package com.ruskserver.deepwither_V2.modules.combat.damage.phases;

import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.item.modifier.SpecialEffect;
import com.ruskserver.deepwither_V2.modules.item.modifier.SpecialEffectService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class SpecialEffectPhase implements DamagePhase {

    private static final double LIFESTEAL_RATIO = 0.03;
    private static final double THORNS_RATIO = 0.05;
    private static final double MANA_SIPHON_CHANCE = 0.05;
    private static final double MANA_SIPHON_AMOUNT = 10.0;
    private static final double BERSERK_MULTIPLIER = 1.20;

    private final SpecialEffectService specialEffectService;
    private final VirtualHealthManager healthManager;
    private final ManaManager manaManager;

    public SpecialEffectPhase(SpecialEffectService specialEffectService,
                              VirtualHealthManager healthManager,
                              ManaManager manaManager) {
        this.specialEffectService = specialEffectService;
        this.healthManager = healthManager;
        this.manaManager = manaManager;
    }

    @Override
    public void process(DamageContext context) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity defender = context.getDefender();
        if (attacker == null || context.getDamage() <= 0.0
                || context.getType() == DamageType.ENVIRONMENTAL) {
            return;
        }

        if (specialEffectService.hasEffect(attacker, SpecialEffect.BERSERK)
                && healthManager.getHealth(attacker) <= healthManager.getMaxHealth(attacker) * 0.5) {
            context.multiplyDamage(BERSERK_MULTIPLIER);
        }

        double finalDamage = context.getDamage();
        if (specialEffectService.hasEffect(attacker, SpecialEffect.LIFESTEAL)) {
            healthManager.heal(attacker, finalDamage * LIFESTEAL_RATIO);
        }
        if (attacker instanceof Player player
                && specialEffectService.hasEffect(attacker, SpecialEffect.MANA_SIPHON)
                && ThreadLocalRandom.current().nextDouble() < MANA_SIPHON_CHANCE) {
            manaManager.restore(player, MANA_SIPHON_AMOUNT);
        }
        if (specialEffectService.hasEffect(defender, SpecialEffect.THORNS)) {
            healthManager.damage(attacker, finalDamage * THORNS_RATIO);
        }
    }
}
