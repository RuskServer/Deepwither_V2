package com.ruskserver.deepwither_V2.modules.combat.damage.phases;

import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.skill.definitions.MartyrdomSkill;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * スキル「殉教」による味方への最終被ダメージ50%肩代わりを処理するダメージフェーズ。
 */
public class MartyrdomPhase implements DamagePhase {

    private final MartyrdomSkill martyrdomSkill;
    private final VirtualHealthManager healthManager;

    public MartyrdomPhase(MartyrdomSkill martyrdomSkill, VirtualHealthManager healthManager) {
        this.martyrdomSkill = martyrdomSkill;
        this.healthManager = healthManager;
    }

    @Override
    public void process(DamageContext context) {
        if (!(context.getDefender() instanceof Player defender)) return;
        if (context.getDamage() <= 0) return;

        Map<UUID, Long> active = martyrdomSkill.getActiveMartyrdom();
        if (active.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : active.entrySet()) {
            UUID martyrId = entry.getKey();
            if (now > entry.getValue()) {
                active.remove(martyrId);
                continue;
            }

            if (martyrId.equals(defender.getUniqueId())) continue;

            Player martyr = Bukkit.getPlayer(martyrId);
            if (martyr == null || !martyr.isOnline() || martyr.isDead()) continue;

            if (martyr.getLocation().distanceSquared(defender.getLocation()) > 7.0 * 7.0) continue;

            double originalDamage = context.getDamage();
            double redirectedDamage = originalDamage * 0.5;

            if (redirectedDamage > 0) {
                context.setDamage(originalDamage * 0.5);
                healthManager.damage(martyr, redirectedDamage);
                break;
            }
        }
    }
}
