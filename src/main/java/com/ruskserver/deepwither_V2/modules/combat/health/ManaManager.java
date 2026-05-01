package com.ruskserver.deepwither_V2.modules.combat.health;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * プレイヤーの現在マナを管理するサービス。
 * 自動回復タスクも内包します。
 */
@Service
public class ManaManager implements Startable {

    private final Map<UUID, Double> currentManaMap = new HashMap<>();
    private final StatManager statManager;
    private final Deepwither_V2 plugin;

    @Inject
    public ManaManager(StatManager statManager, Deepwither_V2 plugin) {
        this.statManager = statManager;
        this.plugin = plugin;
    }

    @Override
    public void start() {
        // 1秒(20ticks)ごとにマナを自動回復するタスク
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                double maxMana = getMaxMana(player);
                if (maxMana <= 0) continue;

                double current = getMana(player);
                if (current < maxMana) {
                    // 最大マナの5%を毎秒回復 (または基礎ステータス MANA_REGEN を作るなど拡張可能)
                    double regenAmount = maxMana * 0.05;
                    double newMana = Math.min(current + regenAmount, maxMana);
                    currentManaMap.put(player.getUniqueId(), newMana);
                    
                    // アクションバーにマナを表示
                    sendManaActionBar(player, newMana, maxMana);
                }
            }
        }, 20L, 20L);
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
        return statManager.getTotalStat(player, StatType.MAX_MANA);
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
        
        sendManaActionBar(player, newMana, getMaxMana(player));
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
        
        sendManaActionBar(player, newMana, max);
    }
    
    private void sendManaActionBar(Player player, double current, double max) {
        player.sendActionBar(Component.text("§b✤ マナ: " + (int)current + " / " + (int)max));
    }
}
