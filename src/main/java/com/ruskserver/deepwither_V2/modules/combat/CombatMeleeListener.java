package com.ruskserver.deepwither_V2.modules.combat;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.WandItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CombatMeleeListener implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final StatManager statManager;
    private final ItemPDCUtil pdcUtil;
    private final ItemManager itemManager;
    private final CombatHitDetectionService hitDetectionService;
    private final DamagePipelineManager damagePipelineManager;
    private final CombatStatsService statsService;

    @Inject
    public CombatMeleeListener(
            StatManager statManager,
            ItemPDCUtil pdcUtil,
            ItemManager itemManager,
            CombatHitDetectionService hitDetectionService,
            DamagePipelineManager damagePipelineManager,
            CombatStatsService statsService
    ) {
        this.statManager = statManager;
        this.pdcUtil = pdcUtil;
        this.itemManager = itemManager;
        this.hitDetectionService = hitDetectionService;
        this.damagePipelineManager = damagePipelineManager;
        this.statsService = statsService;
    }

    @EventHandler
    public void onPlayerArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.isEmpty()) return;
        if (isWand(hand)) return;

        CombatWeaponType type = resolveWeaponType(hand);
        if (type == null) return;

        long now = System.currentTimeMillis();
        double attackSpeed = statManager.getTotalStat(player, StatType.ATTACK_SPEED);
        if (attackSpeed <= 0) attackSpeed = 1.0;
        long cooldownMs = (long) (1000.0 / attackSpeed);
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownMs) return;
        cooldowns.put(player.getUniqueId(), now);

        statsService.recordAttack(player);
        List<LivingEntity> targets = hitDetectionService.detectTargets(player, type);
        if (targets.isEmpty()) {
            playMissEffect(player);
            return;
        }

        for (LivingEntity target : targets) {
            damagePipelineManager.processDamage(player, target, DamageType.PHYSICAL, 0.0, null);
            statsService.recordHit(player, type, Math.max(0.0, statManager.getTotalStat(player, StatType.ATTACK_DAMAGE)));
            playHitSound(target);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaMelee(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && !isWand(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    private boolean isWand(ItemStack itemStack) {
        String itemId = pdcUtil.getItemId(itemStack);
        if (itemId == null) return false;
        CustomItem customItem = itemManager.getCustomItem(itemId);
        return customItem instanceof WandItem;
    }

    private CombatWeaponType resolveWeaponType(ItemStack itemStack) {
        String itemId = pdcUtil.getItemId(itemStack);
        if (itemId == null) return null;
        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (customItem == null) return null;
        return CombatWeaponType.fromItem(customItem);
    }

    private void playMissEffect(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.0f);
    }

    private void playHitSound(LivingEntity target) {
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.0f);
    }
}
