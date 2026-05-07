package com.ruskserver.deepwither_V2.modules.trader.service;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.trader.provider.TraderReputationProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * デイリータスク（モブ討伐）の進行状況を管理するサービス。
 */
@Service
public class DailyTaskService implements Listener {

    private final PlayerDataRepository repository;
    private final TraderReputationService reputationService;
    private final CustomMobManager mobManager;

    /** プレイヤーUUID -> (モブID -> 討伐数) の一時的な進行状況（DBに保存してもよいが、今回はメモリ管理） */
    private final Map<UUID, ActiveTask> activeTasks = new HashMap<>();

    @Inject
    public DailyTaskService(PlayerDataRepository repository, TraderReputationService reputationService, CustomMobManager mobManager) {
        this.repository = repository;
        this.reputationService = reputationService;
        this.mobManager = mobManager;
    }

    /**
     * プレイヤーがタスクを受注しているか確認します。
     */
    public boolean hasTask(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }

    /**
     * タスクを受注します。
     */
    public boolean acceptTask(Player player, String traderId, String mobId, int requiredCount, int rewardRep) {
        if (reputationService.getRemainingTaskSlots(player) <= 0) {
            player.sendMessage("§c本日のタスク受注上限に達しています。");
            return false;
        }
        if (hasTask(player)) {
            player.sendMessage("§c既に他のタスクを受注しています。");
            return false;
        }

        activeTasks.put(player.getUniqueId(), new ActiveTask(traderId, mobId, requiredCount, rewardRep));
        player.sendMessage("§aデイリータスクを受注しました: §e" + mobId + " を " + requiredCount + " 体討伐する");
        return true;
    }

    /**
     * タスクの進捗を取得します。
     */
    public String getProgressString(Player player) {
        ActiveTask task = activeTasks.get(player.getUniqueId());
        if (task == null) return "なし";
        return "§e" + task.targetMobId + "§f: " + task.currentCount + " / " + task.requiredCount;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobKill(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        String mobId = mobManager.getCustomMobId(victim);
        if (mobId == null) return;

        Player killer = victim.getKiller();
        if (killer == null) return;

        ActiveTask task = activeTasks.get(killer.getUniqueId());
        if (task != null && task.targetMobId.equals(mobId)) {
            task.currentCount++;
            if (task.currentCount >= task.requiredCount) {
                completeTask(killer);
            } else {
                killer.sendMessage("§aタスク進捗: " + getProgressString(killer));
            }
        }
    }

    private void completeTask(Player player) {
        ActiveTask task = activeTasks.remove(player.getUniqueId());
        if (task == null) return;

        reputationService.completeTask(player, task.traderId, task.rewardReputation);
        player.sendMessage("§6§lDAILY TASK COMPLETE! §a信用度が " + task.rewardReputation + " 上昇しました。");
    }

    private static class ActiveTask {
        final String traderId;
        final String targetMobId;
        final int requiredCount;
        final int rewardReputation;
        int currentCount = 0;

        ActiveTask(String traderId, String mobId, int requiredCount, int rewardReputation) {
            this.traderId = traderId;
            this.targetMobId = mobId;
            this.requiredCount = requiredCount;
            this.rewardReputation = rewardReputation;
        }
    }
}
