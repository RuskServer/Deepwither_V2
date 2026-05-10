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
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class CombatMeleeListener implements Listener {

    private static final Set<Material> HEAVY_MATERIALS = Set.of(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE);
    private static final Set<Material> SLASH_MATERIALS = Set.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);
    private static final Set<Material> THRUST_MATERIALS = Set.of(Material.TRIDENT);

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
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
            playMissEffect(player, type);
            return;
        }

        for (LivingEntity target : targets) {
            damagePipelineManager.processDamage(player, target, DamageType.PHYSICAL, 0.0, null);
            statsService.recordHit(player, type, Math.max(0.0, statManager.getTotalStat(player, StatType.ATTACK_DAMAGE)));
            playHitEffect(target, type);
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
        if (itemId != null) {
            String lower = itemId.toLowerCase();
            if (lower.contains("spear") || lower.contains("lance") || lower.contains("pike") || lower.contains("thrust")) return CombatWeaponType.THRUST;
            if (lower.contains("axe") || lower.contains("hammer") || lower.contains("heavy")) return CombatWeaponType.HEAVY;
            if (lower.contains("sword") || lower.contains("slash") || lower.contains("blade")) return CombatWeaponType.SLASH;
        }

        Material type = itemStack.getType();
        if (THRUST_MATERIALS.contains(type)) return CombatWeaponType.THRUST;
        if (HEAVY_MATERIALS.contains(type)) return CombatWeaponType.HEAVY;
        if (SLASH_MATERIALS.contains(type)) return CombatWeaponType.SLASH;
        return null;
    }

    private void playMissEffect(Player player, CombatWeaponType type) {
        float pitch = switch (type) {
            case HEAVY -> 0.75f;
            case SLASH -> 1.0f;
            case THRUST -> 1.2f;
        };
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, pitch);
    }

    private void playHitEffect(LivingEntity target, CombatWeaponType type) {
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.0f);
        Particle particle = switch (type) {
            case HEAVY -> Particle.CRIT;
            case SLASH -> Particle.SWEEP_ATTACK;
            case THRUST -> Particle.ELECTRIC_SPARK;
        };
        target.getWorld().spawnParticle(particle, target.getLocation().add(0, 1.0, 0), 8, 0.25, 0.25, 0.25, 0.0);
    }
}
