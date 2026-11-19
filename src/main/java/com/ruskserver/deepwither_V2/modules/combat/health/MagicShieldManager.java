package com.ruskserver.deepwither_V2.modules.combat.health;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PoE Energy Shield 風のマジックシールド（ES）を管理するサービス。
 * <p>
 * ・通常の物理・属性ダメージは HP より先にマジックシールドで吸収する
 * ・深淵（{@code ABYSS}）および確定ダメージはマジックシールドを貫通して HP へ直接通る
 * ・非被弾が {@value #RECHARGE_DELAY_MILLIS}ms 続くと、最大値の 20%/秒（回復速度スタットで増加）で急速リチャージする
 */
@Service
public class MagicShieldManager implements Startable, Stoppable, Listener {

    public static final long RECHARGE_DELAY_MILLIS = 4000L;
    public static final double BASE_RECHARGE_FRACTION_PER_SECOND = 0.20;
    private static final long TICK_INTERVAL = 4L; // 0.2秒ごと

    private final Deepwither_V2 plugin;
    private final StatManager statManager;

    private final Map<UUID, Double> currentShieldMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDamagedAtMap = new ConcurrentHashMap<>();
    private BukkitTask rechargeTask;

    @Inject
    public MagicShieldManager(Deepwither_V2 plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    @Override
    public void start() {
        rechargeTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRecharge, TICK_INTERVAL, TICK_INTERVAL);
    }

    @Override
    public void stop() {
        if (rechargeTask != null) {
            rechargeTask.cancel();
            rechargeTask = null;
        }
        currentShieldMap.clear();
        lastDamagedAtMap.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer().getUniqueId());
    }

    public void cleanup(UUID entityId) {
        currentShieldMap.remove(entityId);
        lastDamagedAtMap.remove(entityId);
    }

    public double getMaxMagicShield(LivingEntity entity) {
        if (entity == null) return 0.0;
        return Math.max(0.0, statManager.getTotalStat(entity, StatType.MAX_MAGIC_SHIELD));
    }

    public double getMagicShield(LivingEntity entity) {
        if (entity == null) return 0.0;
        double max = getMaxMagicShield(entity);
        if (max <= 0.0) {
            currentShieldMap.remove(entity.getUniqueId());
            return 0.0;
        }
        double cur = currentShieldMap.computeIfAbsent(entity.getUniqueId(), id -> max);
        if (cur > max) {
            cur = max;
            currentShieldMap.put(entity.getUniqueId(), cur);
        }
        return Math.max(0.0, cur);
    }

    public void setMagicShield(LivingEntity entity, double amount) {
        if (entity == null) return;
        double max = getMaxMagicShield(entity);
        if (max <= 0.0) {
            currentShieldMap.remove(entity.getUniqueId());
            return;
        }
        currentShieldMap.put(entity.getUniqueId(), Math.max(0.0, Math.min(max, amount)));
    }

    /** 被弾時刻を記録してリチャージ遅延をリセットする */
    public void recordDamageTaken(LivingEntity entity) {
        if (entity != null) {
            lastDamagedAtMap.put(entity.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * シールド吸収可能ダメージをマジックシールドで肩代わりし、吸収しきれなかった残余ダメージを返す。
     */
    public double absorbDamage(LivingEntity entity, double shieldableDamage) {
        if (entity == null || shieldableDamage <= 0.0) return Math.max(0.0, shieldableDamage);
        recordDamageTaken(entity);

        double current = getMagicShield(entity);
        if (current <= 0.0) return shieldableDamage;

        double absorbed = Math.min(current, shieldableDamage);
        currentShieldMap.put(entity.getUniqueId(), current - absorbed);
        return Math.max(0.0, shieldableDamage - absorbed);
    }

    private void tickRecharge() {
        long now = System.currentTimeMillis();
        double dtSec = TICK_INTERVAL / 20.0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) continue;
            double max = getMaxMagicShield(player);
            if (max <= 0.0) continue;

            double current = getMagicShield(player);
            if (current >= max) continue;

            long lastHit = lastDamagedAtMap.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastHit < RECHARGE_DELAY_MILLIS) continue;

            double rateBonus = Math.max(-0.9, statManager.getTotalStat(player, StatType.MAGIC_SHIELD_RECHARGE_RATE) / 100.0);
            double regen = max * BASE_RECHARGE_FRACTION_PER_SECOND * (1.0 + rateBonus) * dtSec;
            if (regen > 0.0) {
                currentShieldMap.put(player.getUniqueId(), Math.min(max, current + regen));
            }
        }
    }
}
