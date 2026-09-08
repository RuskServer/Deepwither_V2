package com.ruskserver.deepwither_V2.modules.combat;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Service
public class CombatStateService implements Listener, Startable, Stoppable {
    private final JavaPlugin plugin;
    private final CombatStateTracker tracker = new CombatStateTracker(() -> System.nanoTime() / 1_000_000);
    private final Map<UUID, Mob> pursuingMobs = new HashMap<>();
    private final Map<UUID, Entity> bosses = new HashMap<>();
    private BukkitTask task;

    @Inject
    public CombatStateService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(Mob.class).forEach(this::trackMob));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 10L);
    }

    public boolean isInCombat(Player player) {
        return tracker.isInCombat(player.getUniqueId());
    }

    public void recordAttack(LivingEntity attacker, LivingEntity defender) {
        if (attacker == null || defender == null || attacker.equals(defender)) return;
        recordActivity(attacker);
        recordActivity(defender);
        joinBoss(attacker, defender);
        joinBoss(defender, attacker);
    }

    public void recordActivity(LivingEntity entity) {
        if (entity instanceof Player player && isActive(player)) tracker.recordActivity(player.getUniqueId());
    }

    public void recordSupport(LivingEntity source, LivingEntity target) {
        if (source instanceof Player caster && target instanceof Player ally
                && isActive(caster) && isActive(ally) && caster.getWorld().equals(ally.getWorld())) {
            tracker.recordSupport(caster.getUniqueId(), ally.getUniqueId());
        }
    }

    public void registerBoss(Entity boss) {
        bosses.put(boss.getUniqueId(), boss);
    }

    public void joinEncounter(Entity boss, Player player) {
        if (!isActive(player) || !boss.getWorld().equals(player.getWorld())) return;
        registerBoss(boss);
        tracker.joinEncounter(boss.getUniqueId(), player.getUniqueId());
    }

    public void endEncounter(UUID bossId) {
        bosses.remove(bossId);
        tracker.endEncounter(bossId);
    }

    private void joinBoss(LivingEntity boss, LivingEntity participant) {
        // ボス参加は命中または対象選択時に登録する。
        if (bosses.containsKey(boss.getUniqueId()) && participant instanceof Player player) {
            joinEncounter(boss, player);
        }
    }

    public void leaveEncounter(UUID bossId, UUID playerId) {
        tracker.leaveEncounter(bossId, playerId);
    }

    public void trackMob(Mob mob) {
        if (mob.getTarget() instanceof Player player && isActive(player)) {
            pursuingMobs.put(mob.getUniqueId(), mob);
            recordActivity(player);
        }
    }

    private void tick() {
        pursuingMobs.values().removeIf(mob -> {
            if (!mob.isValid() || mob.isDead() || !(mob.getTarget() instanceof Player target)
                    || !isActive(target) || !mob.getWorld().equals(target.getWorld())) return true;
            recordActivity(target);
            return false;
        });
        Iterator<Map.Entry<UUID, Entity>> iterator = bosses.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Entity boss = entry.getValue();
            if (!boss.isValid() || boss.isDead()) {
                tracker.endEncounter(entry.getKey());
                iterator.remove();
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(boss.getWorld())) {
                        tracker.leaveEncounter(entry.getKey(), player.getUniqueId());
                    }
                }
            }
        }
        Bukkit.getOnlinePlayers().forEach(player -> tracker.isInCombat(player.getUniqueId()));
    }

    private boolean isActive(Player player) {
        return player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            if (event.getTarget() instanceof Player player && isActive(player)) {
                pursuingMobs.put(mob.getUniqueId(), mob);
                recordActivity(player);
            } else {
                pursuingMobs.remove(mob.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        endEncounter(event.getEntity().getUniqueId());
        pursuingMobs.remove(event.getEntity().getUniqueId());
        if (event.getEntity() instanceof Player player) tracker.clearPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracker.clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // 時間制限は保持し、旧ワールドのボス参加だけ解除する。
        bosses.forEach((id, boss) -> {
            if (!boss.getWorld().equals(event.getPlayer().getWorld())) {
                tracker.leaveEncounter(id, event.getPlayer().getUniqueId());
            }
        });
    }

    @Override
    public void stop() {
        if (task != null) task.cancel();
        tracker.clear();
        pursuingMobs.clear();
        bosses.clear();
    }
}
