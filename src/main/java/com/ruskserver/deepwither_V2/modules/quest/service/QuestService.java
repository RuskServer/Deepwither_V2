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
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.quest.api.Quest;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestReward;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestState;
import com.ruskserver.deepwither_V2.modules.quest.definitions.DailyCollectionQuest;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private static final int MAX_DAILY_COMPLETIONS = 5;

    private final Map<String, Quest> registry = new HashMap<>();

    @Inject
    public QuestService(JavaPlugin plugin, DIContainer container,
                        PlayerDataRepository playerDataRepo,
                        QuestProgressProvider progressProvider,
                        DungeonPortalManager dungeonPortalManager,
                        PortalLocationRepository portalLocationRepo,
                        ItemPDCUtil pdcUtil) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.container = container;
        this.playerDataRepo = playerDataRepo;
        this.progressProvider = progressProvider;
        this.dungeonPortalManager = dungeonPortalManager;
        this.portalLocationRepo = portalLocationRepo;
        this.pdcUtil = pdcUtil;
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
        if (data == null) return new QuestProgress(null, null, 0, "", 0);
        return data.get(QuestProgressProvider.KEY);
    }

    public boolean isAccepted(UUID playerId) {
        QuestProgress progress = getProgress(playerId);
        return !progress.isEmpty() && progress.state() == QuestState.ACCEPTED;
    }

    public int getRemainingDailyCompletions(UUID playerId) {
        QuestProgress progress = getProgress(playerId);
        if (progress.isEmpty()) return MAX_DAILY_COMPLETIONS;
        String today = LocalDate.now().toString();
        if (!today.equals(progress.lastResetDate())) return MAX_DAILY_COMPLETIONS;
        return Math.max(0, MAX_DAILY_COMPLETIONS - progress.dailyCompletions());
    }

    public boolean acceptQuest(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        if (isAccepted(uuid)) {
            player.sendMessage(Component.text("既にクエストを受注しています。", NamedTextColor.RED));
            return false;
        }

        int remaining = getRemainingDailyCompletions(uuid);
        if (remaining <= 0) {
            player.sendMessage(Component.text("今日のクエスト受注可能回数（" + MAX_DAILY_COMPLETIONS + "回）に達しました。", NamedTextColor.RED));
            return false;
        }

        Quest quest = registry.get(questId);
        if (quest == null) {
            player.sendMessage(Component.text("クエストが見つかりません。", NamedTextColor.RED));
            return false;
        }

        String today = LocalDate.now().toString();
        QuestProgress currentProgress = getProgress(uuid);
        int previousCompletions = today.equals(currentProgress.lastResetDate()) ? currentProgress.dailyCompletions() : 0;
        QuestProgress progress = new QuestProgress(questId, QuestState.ACCEPTED, System.currentTimeMillis(), today, previousCompletions);
        PlayerData data = playerDataRepo.get(uuid).orElse(null);
        if (data == null) return false;
        data.set(QuestProgressProvider.KEY, progress);
        playerDataRepo.save(uuid, data);

        player.sendMessage(Component.text("§aクエスト「" + questId + "」を受注しました！"));
        player.sendMessage(Component.text("§7残り受注可能回数: " + (remaining - 1) + "/" + MAX_DAILY_COMPLETIONS));
        return true;
    }

    public boolean checkCompletion(Player player) {
        UUID uuid = player.getUniqueId();
        QuestProgress progress = getProgress(uuid);
        if (progress.isEmpty() || progress.state() != QuestState.ACCEPTED) return false;

        Quest quest = registry.get(progress.questId());
        if (quest == null) return false;

        for (QuestObjective obj : quest.getObjectives()) {
            int has = countItem(player, obj.getItemId());
            if (has < obj.getRequiredAmount()) return false;
        }
        return true;
    }

    public boolean turnInQuest(Player player) {
        UUID uuid = player.getUniqueId();
        QuestProgress progress = getProgress(uuid);
        if (progress.isEmpty() || progress.state() != QuestState.ACCEPTED) {
            player.sendMessage(Component.text("完了できるクエストがありません。", NamedTextColor.RED));
            return false;
        }

        if (!checkCompletion(player)) {
            player.sendMessage(Component.text("まだ必要なアイテムが揃っていません。", NamedTextColor.RED));
            return false;
        }

        Quest quest = registry.get(progress.questId());
        if (quest == null) return false;

        for (QuestObjective obj : quest.getObjectives()) {
            removeItem(player, obj.getItemId(), obj.getRequiredAmount());
        }

        for (QuestReward reward : quest.getRewards()) {
            grantReward(player, reward);
        }

        String today = LocalDate.now().toString();
        int newCount = today.equals(progress.lastResetDate()) ? progress.dailyCompletions() + 1 : 1;
        QuestProgress newProgress = new QuestProgress(progress.questId(), QuestState.TURNED_IN,
                progress.acceptedAt(), today, newCount);
        PlayerData data = playerDataRepo.get(uuid).orElse(null);
        if (data == null) return false;
        data.set(QuestProgressProvider.KEY, newProgress);
        playerDataRepo.save(uuid, data);

        int remaining = MAX_DAILY_COMPLETIONS - newCount;
        player.sendMessage(Component.text("§aクエストを完了しました！ ダンジョン地図を入手しました。"));
        if (remaining > 0) {
            player.sendMessage(Component.text("§7今日あと" + remaining + "回受注できます。"));
        } else {
            player.sendMessage(Component.text("§e今日の受注可能回数を使い切りました。"));
        }
        return true;
    }

    public Map<String, Integer> getCurrentCounts(Player player) {
        UUID uuid = player.getUniqueId();
        QuestProgress progress = getProgress(uuid);
        if (progress.isEmpty()) return Map.of();

        Quest quest = registry.get(progress.questId());
        if (quest == null) return Map.of();

        Map<String, Integer> counts = new HashMap<>();
        for (QuestObjective obj : quest.getObjectives()) {
            counts.put(obj.getItemId(), countItem(player, obj.getItemId()));
        }
        return counts;
    }

    public Quest getCurrentQuest(Player player) {
        QuestProgress progress = getProgress(player.getUniqueId());
        if (progress.isEmpty()) return null;
        return registry.get(progress.questId());
    }

    private int countItem(Player player, String itemId) {
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

    private void removeItem(Player player, String itemId, int amount) {
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
        String dungeonId = reward.getDungeonId();
        List<PortalLocation> portals = portalLocationRepo.findByDungeonId(dungeonId);
        if (portals.isEmpty()) {
            log.warning("[QuestService] ダンジョン '" + dungeonId + "' に対応するポータルが見つかりません。");
            player.sendMessage(Component.text("ダンジョンポータルが設定されていません。運営にお問い合わせください。", NamedTextColor.RED));
            return;
        }

        PortalLocation chosen = portals.get(ThreadLocalRandom.current().nextInt(portals.size()));
        ItemStack map = dungeonPortalManager.createMapItem(dungeonId, chosen);
        if (map == null) return;

        var result = player.getInventory().addItem(map);
        if (!result.isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), map);
        }
    }
}
