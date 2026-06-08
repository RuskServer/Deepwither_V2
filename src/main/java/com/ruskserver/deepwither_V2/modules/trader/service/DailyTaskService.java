package com.ruskserver.deepwither_V2.modules.trader.service;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.modules.mob.event.CustomMobDeathEvent;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.mob.region.MobRegion;
import com.ruskserver.deepwither_V2.modules.mob.region.MobRegionConfig;
import com.ruskserver.deepwither_V2.modules.mob.region.SpawnEntry;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.trader.provider.TraderReputationProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * デイリータスク（モブ討伐）の進行状況を管理するサービス。
 */
@Service
public class DailyTaskService implements Listener, PlayerLifecycleTask {

    private final PlayerDataRepository repository;
    private final TraderReputationService reputationService;
    private final TraderService traderService;
    private final CustomMobManager mobManager;
    private final MobRegionConfig regionConfig;
    private final PlayerManager playerManager;
    private final Random random = new Random();

    /** タスク報酬の基礎経験値 */
    private static final int BASE_EXP_REWARD = 200;

    /**
     * レベルに応じてスケーリングされた経験値報酬を計算します。
     * 式: BASE_EXP_REWARD * (1.0 + level * 0.05)
     * Lv1 → 210, Lv20 → 400, Lv50 → 700, Lv100 → 1200
     */
    private int calcExpReward(int level) {
        return (int) Math.round(BASE_EXP_REWARD * (1.0 + level * 0.05));
    }

    /** プレイヤーUUID -> (モブID -> 討伐数) の一時的な進行状況（DBに保存してもよいが、今回はメモリ管理） */
    private final Map<UUID, ActiveTask> activeTasks = new HashMap<>();

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.QUIT);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.CLEANUP;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> activeTasks.remove(context.playerId()));
    }

    @Inject
    public DailyTaskService(PlayerDataRepository repository, TraderReputationService reputationService, TraderService traderService, CustomMobManager mobManager, MobRegionConfig regionConfig, PlayerManager playerManager) {
        this.repository = repository;
        this.reputationService = reputationService;
        this.traderService = traderService;
        this.mobManager = mobManager;
        this.regionConfig = regionConfig;
        this.playerManager = playerManager;
    }

    /**
     * プレイヤーがタスクを受注しているか確認します。
     */
    public boolean hasTask(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }

    /**
     * プレイヤーの現在位置に基づいてタスクを生成・受注させます。
     *
     * @param player   プレイヤー
     * @param traderId トレーダーID
     * @return 受注に成功した場合は true
     */
    public boolean acceptTask(Player player, String traderId) {
        if (reputationService.getRemainingTaskSlots(player) <= 0) {
            player.sendMessage("§c本日のタスク受注上限に達しています。");
            return false;
        }
        if (hasTask(player)) {
            player.sendMessage("§c既に他のタスクを受注しています。");
            return false;
        }

        // 現在のリージョンを取得
        MobRegion currentRegion = regionConfig.getRegions().stream()
                .filter(r -> r.contains(player.getLocation()))
                .findFirst()
                .orElse(null);

        String targetMobId = "ghoul"; // デフォルト
        int requiredCount = 5 + random.nextInt(11); // 5~15体

        if (currentRegion != null && !currentRegion.isSafeZone() && !currentRegion.spawnTable().isEmpty()) {
            targetMobId = selectRandomMob(currentRegion);
        }

        String displayName = mobManager.getDisplayName(targetMobId);
        activeTasks.put(player.getUniqueId(), new ActiveTask(traderId, targetMobId, displayName, requiredCount));
        player.sendMessage("§aデイリータスクを受注しました: §e" + displayName + " を " + requiredCount + " 体討伐する");
        return true;
    }

    private String selectRandomMob(MobRegion region) {
        int totalWeight = region.getTotalWeight();
        if (totalWeight <= 0) return "ghoul";

        int r = random.nextInt(totalWeight);
        int current = 0;
        for (SpawnEntry entry : region.spawnTable()) {
            current += entry.weight();
            if (r < current) {
                return entry.mobId();
            }
        }
        return "ghoul";
    }

    /**
     * タスクの進捗を取得します。
     */
    public String getProgressString(Player player) {
        ActiveTask task = activeTasks.get(player.getUniqueId());
        if (task == null) return "なし";
        return "§e" + task.targetMobDisplayName + "§f: " + task.currentCount + " / " + task.requiredCount;
    }

    /**
     * 現在進行中のタスクの対象モブ名を取得します。
     */
    public String getActiveTaskMobName(Player player) {
        ActiveTask task = activeTasks.get(player.getUniqueId());
        return task != null ? task.targetMobDisplayName : null;
    }

    /**
     * 現在進行中のタスクの進捗状況を [現在数, 目標数] の配列で取得します。
     */
    public int[] getActiveTaskProgress(Player player) {
        ActiveTask task = activeTasks.get(player.getUniqueId());
        if (task == null) return new int[]{0, 0};
        return new int[]{task.currentCount, task.requiredCount};
    }

    @EventHandler
    public void onCustomMobKill(CustomMobDeathEvent event) {
        Player killer = event.getKiller();
        if (killer == null || !killer.isOnline()) return;

        String mobId = event.getMobId();
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

        // 報酬の計算 (ゴールド: 1000~4000, 信用度: 100~400)
        int goldReward = 1000 + random.nextInt(3001);
        int creditReward = 100 + random.nextInt(301);

        // 経験値報酬（プレイヤーレベルでスケーリング）
        int playerLevel = playerManager.getPlayerLevel(player);
        int expReward = calcExpReward(playerLevel);

        // 報酬の付与
        traderService.depositMoney(player, goldReward);
        reputationService.completeTask(player, task.traderId, creditReward);
        playerManager.addExp(player, expReward);

        // メッセージの送信
        player.sendMessage(Component.text("タスク完了！", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" " + task.traderId + "のタスクをクリアしました！", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        
        player.sendMessage(Component.text("報酬: ", NamedTextColor.GOLD)
                .append(Component.text(traderService.formatMoney(goldReward), NamedTextColor.GOLD))
                .append(Component.text(" と ", NamedTextColor.WHITE))
                .append(Component.text(creditReward, NamedTextColor.AQUA))
                .append(Component.text(" 信用度を獲得！", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
    }

    private static class ActiveTask {
        final String traderId;
        final String targetMobId;
        final String targetMobDisplayName;
        final int requiredCount;
        int currentCount = 0;

        ActiveTask(String traderId, String mobId, String displayName, int requiredCount) {
            this.traderId = traderId;
            this.targetMobId = mobId;
            this.targetMobDisplayName = displayName;
            this.requiredCount = requiredCount;
        }
    }
}
