package com.ruskserver.deepwither_V2.modules.combat.health;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.CombatStateService;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーの現在マナを管理するサービス。
 * 自動回復タスクも内包します。
 */
@Service
public class ManaManager implements Startable, Stoppable, Listener {

    private static final double DEFAULT_MAX_MANA = 100.0;

    private final Map<UUID, Double> currentManaMap = new ConcurrentHashMap<>();
    private final StatManager statManager;
    private final CombatStateService combatState;
    private final Deepwither_V2 plugin;
    private BukkitTask regenTask;

    @Inject
    public ManaManager(StatManager statManager, CombatStateService combatState, Deepwither_V2 plugin) {
        this.statManager = statManager;
        this.combatState = combatState;
        this.plugin = plugin;
    }

    @Override
    public void start() {
        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                double maxMana = getMaxMana(player);
                if (maxMana <= 0) continue;

                double current = getMana(player);
                if (current < maxMana) {
                    double newMana = ManaRegeneration.regenerate(current, maxMana, isInCombat(player));
                    currentManaMap.put(player.getUniqueId(), newMana);
                }
            }
        }, 20L, 20L);
    }

    @Override
    public void stop() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
        currentManaMap.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        currentManaMap.remove(event.getPlayer().getUniqueId());
    }

    public double getMana(Player player) {
        UUID id = player.getUniqueId();
        if (!currentManaMap.containsKey(id)) {
            double maxMana = getMaxMana(player);
            currentManaMap.put(id, maxMana);
            return maxMana;
        }
        return currentManaMap.get(id);
    }

    public double getMaxMana(Player player) {
        double maxMana = statManager.getTotalStat(player, StatType.MAX_MANA);
        return maxMana > 0 ? maxMana : DEFAULT_MAX_MANA;
    }

    public boolean isInCombat(Player player) {
        return combatState.isInCombat(player);
    }

    public double getRegenerationPerSecond(Player player) {
        return getMaxMana(player) * ManaRegeneration.rate(isInCombat(player));
    }

    public String getCombatStatus(Player player) {
        if (isInCombat(player)) return "戦闘中";
        return getMana(player) < getMaxMana(player) ? "戦闘外回復中" : "戦闘外";
    }

    /**
     * マナを消費します。足りない場合は false を返します。
     */
    public boolean consume(Player player, double amount) {
        double current = getMana(player);
        if (current < amount) {
            return false;
        }
        double newMana = current - amount;
        currentManaMap.put(player.getUniqueId(), newMana);
        
        return true;
    }

    /**
     * マナを回復します。
     */
    public void restore(Player player, double amount) {
        double current = getMana(player);
        double max = getMaxMana(player);
        double newMana = Math.min(current + amount, max);
        currentManaMap.put(player.getUniqueId(), newMana);
    }
}
