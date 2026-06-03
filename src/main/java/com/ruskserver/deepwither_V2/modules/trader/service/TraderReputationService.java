package com.ruskserver.deepwither_V2.modules.trader.service;

import com.ruskserver.deepwither_V2.core.database.player.PlayerData;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.trader.provider.TraderReputationProvider;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * トレーダーの信用度とデイリータスクを管理するサービス。
 */
@Service
public class TraderReputationService {

    private final PlayerDataRepository repository;
    public static final int MAX_DAILY_TASKS = 5;

    @Inject
    public TraderReputationService(PlayerDataRepository repository) {
        this.repository = repository;
    }

    /**
     * 本日の合計タスク完了数を取得します。
     */
    public int getTotalCompletedTasksToday(Player player) {
        return repository.get(player.getUniqueId())
                .map(data -> {
                    checkAndResetDaily(data);
                    return data.get(TraderReputationProvider.KEY).getCompletedTasksToday();
                })
                .orElse(0);
    }

    /**
     * 指定したトレーダーに対するプレイヤーの信用度を取得します。
     */
    public int getReputation(Player player, String traderId) {
        return repository.get(player.getUniqueId())
                .map(data -> data.get(TraderReputationProvider.KEY).getReputations().getOrDefault(traderId, 0))
                .orElse(0);
    }

    /**
     * 信用度を増減させます。
     */
    public void addReputation(Player player, String traderId, int amount) {
        repository.get(player.getUniqueId()).ifPresent(data -> {
            TraderReputationProvider.ReputationData repData = data.get(TraderReputationProvider.KEY);
            Map<String, Integer> reps = repData.getReputations();
            reps.put(traderId, reps.getOrDefault(traderId, 0) + amount);
            data.markDirty(TraderReputationProvider.KEY);
            repository.save(player.getUniqueId(), data);
        });
    }

    /**
     * デイリータスクの残り受注回数を取得します。
     */
    public int getRemainingTaskSlots(Player player) {
        return repository.get(player.getUniqueId())
                .map(data -> {
                    checkAndResetDaily(data);
                    return MAX_DAILY_TASKS - data.get(TraderReputationProvider.KEY).getCompletedTasksToday();
                })
                .orElse(0);
    }

    /**
     * タスクを完了状態にします。
     */
    public void completeTask(Player player, String traderId, int reputationReward) {
        repository.get(player.getUniqueId()).ifPresent(data -> {
            checkAndResetDaily(data);
            TraderReputationProvider.ReputationData repData = data.get(TraderReputationProvider.KEY);
            
            if (repData.getCompletedTasksToday() < MAX_DAILY_TASKS) {
                repData.setCompletedTasksToday(repData.getCompletedTasksToday() + 1);
                addReputation(player, traderId, reputationReward);
                // addReputation 内で save されるが、一応ここでも dirty マーク
                data.markDirty(TraderReputationProvider.KEY);
                repository.save(player.getUniqueId(), data);
            }
        });
    }

    /**
     * 日付をチェックし、必要であればデイリーカウントをリセットします。
     */
    private void checkAndResetDaily(PlayerData data) {
        TraderReputationProvider.ReputationData repData = data.get(TraderReputationProvider.KEY);
        long lastReset = repData.getLastResetTimestamp();
        
        LocalDate lastResetDate = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(lastReset), ZoneId.systemDefault());
        LocalDate today = LocalDate.now();

        if (today.isAfter(lastResetDate)) {
            repData.setCompletedTasksToday(0);
            repData.setLastResetTimestamp(System.currentTimeMillis());
            data.markDirty(TraderReputationProvider.KEY);
        }
    }
}
