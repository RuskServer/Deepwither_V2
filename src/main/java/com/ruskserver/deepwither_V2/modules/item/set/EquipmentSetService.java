package com.ruskserver.deepwither_V2.modules.item.set;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.character.event.CharacterSelectEvent;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.mob.event.CustomMobDeathEvent;
import com.ruskserver.deepwither_V2.modules.revival.PlayerDownEvent;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EquipmentSetService implements Listener, Stoppable {

    public static final String DEFENDER_TWO_PIECE_SOURCE = "set_ghoul_defender_2pc";
    private static final String ATTACKER_FRENZY_DAMAGE_SOURCE = "set_ghoul_attacker_frenzy_damage";
    private static final String ATTACKER_FRENZY_SPEED_SOURCE = "set_ghoul_attacker_frenzy_speed";
    private static final String DEFENDER_REGEN_DEFENSE_SOURCE = "set_ghoul_defender_regen_defense";
    private static final long ATTACKER_FRENZY_DURATION_MILLIS = 6_000L;
    private static final long ATTACKER_FRENZY_COOLDOWN_MILLIS = 10_000L;
    private static final long DEFENDER_REGEN_DURATION_MILLIS = 5_000L;
    private static final long DEFENDER_REGEN_COOLDOWN_MILLIS = 30_000L;

    private final JavaPlugin plugin;
    private final ItemPDCUtil pdcUtil;
    private final StatManager statManager;
    private final VirtualHealthManager healthManager;
    private final Map<UUID, Long> attackerFrenzyCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> attackerFrenzyExpirations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> defenderRegenCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> defenderRegenExpirations = new ConcurrentHashMap<>();

    @Inject
    public EquipmentSetService(JavaPlugin plugin, ItemPDCUtil pdcUtil, StatManager statManager,
                               VirtualHealthManager healthManager) {
        this.plugin = plugin;
        this.pdcUtil = pdcUtil;
        this.statManager = statManager;
        this.healthManager = healthManager;
    }

    public int countPieces(Player player, EquipmentSet set) {
        Set<String> equippedIds = new HashSet<>();
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null || item.isEmpty()) continue;
            String itemId = pdcUtil.getItemId(item);
            if (set.getItemIds().contains(itemId)) {
                equippedIds.add(itemId);
            }
        }
        return equippedIds.size();
    }

    public void processDefenderEffects(DamageContext context) {
        if (!(context.getDefender() instanceof Player defender)) return;
        if (!isPhysical(context.getType())) return;
        if (countPieces(defender, EquipmentSet.GHOUL_DEFENDER) < 4) return;

        double maxHealth = healthManager.getMaxHealth(defender);
        double currentHealth = healthManager.getHealth(defender);
        double threshold = maxHealth * 0.35;
        if (currentHealth <= threshold || currentHealth - context.getDamage() > threshold) return;

        long now = System.currentTimeMillis();
        UUID playerId = defender.getUniqueId();
        if (defenderRegenCooldowns.getOrDefault(playerId, 0L) > now) return;

        defenderRegenCooldowns.put(playerId, now + DEFENDER_REGEN_COOLDOWN_MILLIS);
        defenderRegenExpirations.put(playerId, now + DEFENDER_REGEN_DURATION_MILLIS);
        healthManager.heal(defender, maxHealth * 0.10);
        statManager.setModifier(playerId, StatType.DEFENSE, DEFENDER_REGEN_DEFENSE_SOURCE,
                0.15, ModifierType.MULTIPLICATIVE);
        playDefenderRegenFeedback(defender);
        scheduleDefenderModifierRemoval(defender);
    }

    public void processResolvedAttackerEffects(DamageContext context) {
        if (!(context.getAttacker() instanceof Player attacker)) return;
        if (context.getDefender() instanceof Player) return;
        if (!isPhysical(context.getType()) || context.getDamage() <= 0.0) return;
        if (countPieces(attacker, EquipmentSet.GHOUL_ATTACKER) < 2) return;
        healthManager.heal(attacker, context.getDamage() * 0.02);
    }

    @EventHandler
    public void onCustomMobDeath(CustomMobDeathEvent event) {
        Player killer = event.getKiller();
        if (killer == null || countPieces(killer, EquipmentSet.GHOUL_ATTACKER) < 4) return;

        UUID playerId = killer.getUniqueId();
        long now = System.currentTimeMillis();
        if (attackerFrenzyCooldowns.getOrDefault(playerId, 0L) > now) return;

        attackerFrenzyCooldowns.put(playerId, now + ATTACKER_FRENZY_COOLDOWN_MILLIS);
        attackerFrenzyExpirations.put(playerId, now + ATTACKER_FRENZY_DURATION_MILLIS);
        statManager.setModifier(playerId, StatType.ATTACK_DAMAGE, ATTACKER_FRENZY_DAMAGE_SOURCE,
                0.15, ModifierType.MULTIPLICATIVE);
        statManager.setModifier(playerId, StatType.SPEED, ATTACKER_FRENZY_SPEED_SOURCE,
                10.0, ModifierType.ADDITIVE);
        playAttackerFrenzyFeedback(killer);
        scheduleAttackerModifierRemoval(killer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupState(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDown(PlayerDownEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler
    public void onCharacterSelect(CharacterSelectEvent event) {
        cleanup(event.getPlayer());
    }

    @Override
    public void stop() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeTemporaryModifiers(player.getUniqueId());
        }
        attackerFrenzyCooldowns.clear();
        attackerFrenzyExpirations.clear();
        defenderRegenCooldowns.clear();
        defenderRegenExpirations.clear();
    }

    private boolean isPhysical(DamageType type) {
        return type == DamageType.PHYSICAL || type == DamageType.RANGED;
    }

    private void scheduleAttackerModifierRemoval(Player player) {
        UUID playerId = player.getUniqueId();
        long expectedExpiration = attackerFrenzyExpirations.getOrDefault(playerId, 0L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (attackerFrenzyExpirations.getOrDefault(playerId, 0L) != expectedExpiration) return;
            attackerFrenzyExpirations.remove(playerId);
            statManager.removeModifier(playerId, StatType.ATTACK_DAMAGE, ATTACKER_FRENZY_DAMAGE_SOURCE);
            statManager.removeModifier(playerId, StatType.SPEED, ATTACKER_FRENZY_SPEED_SOURCE);
        }, ATTACKER_FRENZY_DURATION_MILLIS / 50L);
    }

    private void scheduleDefenderModifierRemoval(Player player) {
        UUID playerId = player.getUniqueId();
        long expectedExpiration = defenderRegenExpirations.getOrDefault(playerId, 0L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (defenderRegenExpirations.getOrDefault(playerId, 0L) != expectedExpiration) return;
            defenderRegenExpirations.remove(playerId);
            statManager.removeModifier(playerId, StatType.DEFENSE, DEFENDER_REGEN_DEFENSE_SOURCE);
        }, DEFENDER_REGEN_DURATION_MILLIS / 50L);
    }

    private void cleanup(Player player) {
        UUID playerId = player.getUniqueId();
        removeTemporaryModifiers(playerId);
        cleanupState(playerId);
    }

    private void cleanupState(UUID playerId) {
        attackerFrenzyCooldowns.remove(playerId);
        attackerFrenzyExpirations.remove(playerId);
        defenderRegenCooldowns.remove(playerId);
        defenderRegenExpirations.remove(playerId);
    }

    private void removeTemporaryModifiers(UUID playerId) {
        statManager.removeModifier(playerId, StatType.ATTACK_DAMAGE, ATTACKER_FRENZY_DAMAGE_SOURCE);
        statManager.removeModifier(playerId, StatType.SPEED, ATTACKER_FRENZY_SPEED_SOURCE);
        statManager.removeModifier(playerId, StatType.DEFENSE, DEFENDER_REGEN_DEFENSE_SOURCE);
    }

    private void playAttackerFrenzyFeedback(Player player) {
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1.0, 0),
                18, 0.45, 0.65, 0.45, 0.03);
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.8f, 0.65f);
    }

    private void playDefenderRegenFeedback(Player player) {
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1.0, 0),
                24, 0.55, 0.8, 0.55, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.7f, 0.7f);
    }
}
