package com.ruskserver.deepwither_V2.modules.quest.service;

import com.ruskserver.deepwither_V2.core.database.player.PlayerData;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.dungeon.portal.DungeonPortalManager;
import com.ruskserver.deepwither_V2.modules.dungeon.portal.PortalLocation;
import com.ruskserver.deepwither_V2.modules.dungeon.portal.PortalLocationRepository;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.quest.api.*;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider.QuestEntry;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

@Service
public class QuestService implements Startable {

    private final JavaPlugin plugin;
    private final Logger log;
    private final DIContainer container;
    private final PlayerDataRepository playerDataRepo;
    private final QuestProgressProvider progressProvider;
    private final DungeonPortalManager dungeonPortalManager;
    private final PortalLocationRepository portalLocationRepo;
    private final ItemPDCUtil pdcUtil;
    private final ItemManager itemManager;

    private static final int MAX_DAILY_COMPLETIONS = 5;

    private final Map<String, Quest> registry = new HashMap<>();

    @Inject
    public QuestService(JavaPlugin plugin, DIContainer container,
                        PlayerDataRepository playerDataRepo,
                        QuestProgressProvider progressProvider,
                        DungeonPortalManager dungeonPortalManager,
                        PortalLocationRepository portalLocationRepo,
                        ItemPDCUtil pdcUtil,
                        ItemManager itemManager) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.container = container;
        this.playerDataRepo = playerDataRepo;
        this.progressProvider = progressProvider;
        this.dungeonPortalManager = dungeonPortalManager;
        this.portalLocationRepo = portalLocationRepo;
        this.pdcUtil = pdcUtil;
        this.itemManager = itemManager;
    }

    @Override
    public void start() {
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof Quest quest) {
                registry.put(quest.getId(), quest);
            }
        }
        log.info("[QuestService] " + registry.size() + " 個のクエスト定義を登録しました。");
    }

    public Quest getQuest(String id) {
        return registry.get(id);
    }

    public List<Quest> getAllQuests() {
        return List.copyOf(registry.values());
    }

    public QuestProgress getProgress(UUID playerId) {
        PlayerData data = playerDataRepo.get(playerId).orElse(null);
        if (data == null) return QuestProgress.empty();
        return data.get(QuestProgressProvider.KEY);
    }

    public QuestState getState(UUID playerId, String questId) {
        QuestProgress progress = getProgress(playerId);
        return progress.getState(questId);
    }

    public boolean isAccepted(UUID playerId, String questId) {
        return getState(playerId, questId) == QuestState.ACCEPTED;
    }

    public boolean isCompleted(UUID playerId, String questId) {
        QuestState state = getState(playerId, questId);
        return state == QuestState.COMPLETED || state == QuestState.TURNED_IN;
    }

    // 後方互換性用
    public boolean isAccepted(UUID playerId) {
        QuestProgress progress = getProgress(playerId);
        if (progress.entries() == null) return false;
        return progress.entries().values().stream()
                .anyMatch(e -> e.state() == QuestState.ACCEPTED);
    }

    public int getRemainingDailyCompletions(UUID playerId) {
        QuestProgress progress = getProgress(playerId);
        if (progress.isEmpty()) return MAX_DAILY_COMPLETIONS;
        String today = LocalDate.now().toString();
        if (!today.equals(progress.lastResetDate())) return MAX_DAILY_COMPLETIONS;
        return Math.max(0, MAX_DAILY_COMPLETIONS - progress.dailyCompletions());
    }

    public List<Quest> getAcceptedQuests(Player player) {
        UUID uuid = player.getUniqueId();
        QuestProgress progress = getProgress(uuid);
        if (progress.entries() == null) return List.of();

        List<Quest> result = new ArrayList<>();
        for (QuestEntry entry : progress.entries().values()) {
            if (entry.state() == QuestState.ACCEPTED) {
                Quest quest = registry.get(entry.questId());
                if (quest != null) {
                    result.add(quest);
                }
            }
        }
        return result;
    }

    public boolean acceptQuest(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        Quest quest = registry.get(questId);
        if (quest == null) {
            player.sendMessage(Component.text("クエストが見つかりません。", NamedTextColor.RED));
            return false;
        }

        QuestState currentState = getState(uuid, questId);
        if (currentState == QuestState.ACCEPTED) {
            player.sendMessage(Component.text("既にこのクエストを受注しています。", NamedTextColor.RED));
            return false;
        }

        if (!quest.isRepeatable() && (currentState == QuestState.COMPLETED || currentState == QuestState.TURNED_IN)) {
            player.sendMessage(Component.text("このクエストは既に完了しています。", NamedTextColor.RED));
            return false;
        }

        if (quest.getCategory() == QuestCategory.DAILY) {
            int remaining = getRemainingDailyCompletions(uuid);
            if (remaining <= 0) {
                player.sendMessage(Component.text("今日のデイリークエスト受注上限に達しました。", NamedTextColor.RED));
                return false;
            }
        }

        String today = LocalDate.now().toString();
        QuestProgress currentProgress = getProgress(uuid);
        int previousCompletions = today.equals(currentProgress.lastResetDate()) ? currentProgress.dailyCompletions() : 0;

        QuestEntry newEntry = new QuestEntry(questId, QuestState.ACCEPTED, Map.of(), System.currentTimeMillis(), 0L);
        QuestProgress newProgress = currentProgress.withEntry(newEntry).withDaily(today, previousCompletions);

        PlayerData data = playerDataRepo.get(uuid).orElse(null);
        if (data == null) return false;
        data.set(QuestProgressProvider.KEY, newProgress);
        playerDataRepo.save(uuid, data);

        player.sendMessage(Component.text("§aクエスト「" + quest.getTitle() + "」を受注しました！"));
        return true;
    }

    public boolean checkCompletion(Player player, String questId) {
        Quest quest = registry.get(questId);
        if (quest == null) return false;

        UUID uuid = player.getUniqueId();
        QuestProgress progress = getProgress(uuid);
        QuestEntry entry = progress.getEntry(questId);
        if (entry == null || entry.state() != QuestState.ACCEPTED) return false;

        for (QuestObjective obj : quest.getObjectives()) {
            int count;
            if (obj.getType() == ObjectiveType.COLLECT_ITEM && obj.getItemId() != null) {
                count = countItem(player, obj.getItemId());
            } else {
                count = entry.getCounter(obj.getProgressKey());
            }
            if (!obj.isCompleted(count, player)) {
                return false;
            }
        }
        return true;
    }

    // 後方互換用
    public boolean checkCompletion(Player player) {
        Quest quest = getCurrentQuest(player);
        if (quest == null) return false;
        return checkCompletion(player, quest.getId());
    }

    public boolean turnInQuest(Player player, String questId) {
        Quest quest = registry.get(questId);
        if (quest == null) return false;

        UUID uuid = player.getUniqueId();
        QuestProgress progress = getProgress(uuid);
        QuestEntry entry = progress.getEntry(questId);
        if (entry == null || entry.state() != QuestState.ACCEPTED) {
            player.sendMessage(Component.text("完了できるクエストがありません。", NamedTextColor.RED));
            return false;
        }

        if (!checkCompletion(player, questId)) {
            player.sendMessage(Component.text("まだ目標を達成していません。", NamedTextColor.RED));
            return false;
        }

        // 収集アイテムの消費
        for (QuestObjective obj : quest.getObjectives()) {
            if (obj.getType() == ObjectiveType.COLLECT_ITEM && obj.getItemId() != null) {
                removeItem(player, obj.getItemId(), obj.getRequiredAmount());
            }
        }

        // 報酬の付与
        for (QuestReward reward : quest.getRewards()) {
            grantReward(player, reward);
        }

        String today = LocalDate.now().toString();
        int newCompletions = today.equals(progress.lastResetDate()) ? progress.dailyCompletions() : 0;
        if (quest.getCategory() == QuestCategory.DAILY) {
            newCompletions++;
        }

        QuestEntry updatedEntry = entry.withState(QuestState.COMPLETED);
        QuestProgress newProgress = progress.withEntry(updatedEntry).withDaily(today, newCompletions);

        PlayerData data = playerDataRepo.get(uuid).orElse(null);
        if (data == null) return false;
        data.set(QuestProgressProvider.KEY, newProgress);
        playerDataRepo.save(uuid, data);

        player.sendMessage(Component.text("§aクエスト「" + quest.getTitle() + "」を完了しました！"));
        return true;
    }

    // 後方互換用
    public boolean turnInQuest(Player player) {
        Quest quest = getCurrentQuest(player);
        if (quest == null) return false;
        return turnInQuest(player, quest.getId());
    }

    public void incrementProgress(Player player, String progressKey, int amount) {
        UUID uuid = player.getUniqueId();
        QuestProgress progress = getProgress(uuid);
        if (progress.entries() == null || progress.entries().isEmpty()) return;

        boolean modified = false;
        QuestProgress updatedProgress = progress;

        for (QuestEntry entry : progress.entries().values()) {
            if (entry.state() != QuestState.ACCEPTED) continue;

            Quest quest = registry.get(entry.questId());
            if (quest == null) continue;

            for (QuestObjective obj : quest.getObjectives()) {
                if (obj.getProgressKey().equals(progressKey)) {
                    int current = entry.getCounter(progressKey);
                    if (current < obj.getRequiredAmount()) {
                        QuestEntry newEntry = entry.withIncrementedCounter(progressKey, amount);
                        updatedProgress = updatedProgress.withEntry(newEntry);
                        modified = true;

                        int newCount = current + amount;
                        if (newCount >= obj.getRequiredAmount()) {
                            player.sendMessage(Component.text("§a【目標達成】 " + quest.getTitle() + ": " + obj.getDescription()));
                        }
                    }
                }
            }
        }

        if (modified) {
            PlayerData data = playerDataRepo.get(uuid).orElse(null);
            if (data != null) {
                data.set(QuestProgressProvider.KEY, updatedProgress);
                playerDataRepo.save(uuid, data);
            }
        }
    }

    public Quest getCurrentQuest(Player player) {
        List<Quest> accepted = getAcceptedQuests(player);
        return accepted.isEmpty() ? null : accepted.get(0);
    }

    public Map<String, Integer> getCurrentCounts(Player player) {
        Quest quest = getCurrentQuest(player);
        if (quest == null) return Map.of();

        Map<String, Integer> counts = new HashMap<>();
        for (QuestObjective obj : quest.getObjectives()) {
            if (obj.getItemId() != null) {
                counts.put(obj.getItemId(), countItem(player, obj.getItemId()));
            }
        }
        return counts;
    }

    public int countItem(Player player, String itemId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String id = pdcUtil.getItemId(item);
            if (itemId.equals(id)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public void removeItem(Player player, String itemId, int amount) {
        int remaining = amount;
        var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            String id = pdcUtil.getItemId(item);
            if (itemId.equals(id)) {
                int toRemove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - toRemove);
                remaining -= toRemove;
                if (item.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    private void grantReward(Player player, QuestReward reward) {
        // ダンジョン地図報酬
        if (reward.getDungeonId() != null) {
            String dungeonId = reward.getDungeonId();
            List<PortalLocation> portals = portalLocationRepo.findByDungeonId(dungeonId);
            if (portals.isEmpty()) {
                log.warning("[QuestService] ダンジョン '" + dungeonId + "' に対応するポータルが見つかりません。");
                player.sendMessage(Component.text("ダンジョンポータルが設定されていません。", NamedTextColor.RED));
            } else {
                PortalLocation chosen = portals.get(ThreadLocalRandom.current().nextInt(portals.size()));
                ItemStack map = dungeonPortalManager.createMapItem(dungeonId, chosen);
                if (map != null) {
                    giveOrDrop(player, map);
                }
            }
        }

        // アイテム報酬
        if (reward.getItemId() != null) {
            ItemStack item = itemManager.generate(reward.getItemId());
            if (item != null) {
                if (reward.getAmount() > 1) {
                    item.setAmount(reward.getAmount());
                }
                giveOrDrop(player, item);
                player.sendMessage(Component.text("§a報酬を獲得しました: " + reward.getDescription()));
            }
        }

        // 独自grant呼び出し
        reward.grant(player);
    }

    public void giveOrDrop(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        }
    }
}
