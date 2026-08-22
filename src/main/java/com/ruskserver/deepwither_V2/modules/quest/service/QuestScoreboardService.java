package com.ruskserver.deepwither_V2.modules.quest.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.quest.api.ObjectiveType;
import com.ruskserver.deepwither_V2.modules.quest.api.Quest;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider.QuestEntry;
import com.ruskserver.deepwither_V2.modules.quest.provider.QuestProgressProvider.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestScoreboardService implements Startable {

    private static final String OBJECTIVE_NAME = "dw_quest_sb";

    private final JavaPlugin plugin;
    private final QuestService questService;
    private final CharacterNameTagService nameTagService;

    @Inject
    public QuestScoreboardService(JavaPlugin plugin, QuestService questService, CharacterNameTagService nameTagService) {
        this.plugin = plugin;
        this.questService = questService;
        this.nameTagService = nameTagService;
    }

    @Override
    public void start() {
        // 定期更新タスク（アイテム所持数の変動等に追従するため1秒毎に更新）
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 20L, 20L);
    }

    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
            nameTagService.refresh(player);
        }

        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if (obj == null) {
            obj = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY,
                    Component.text("【進行中のクエスト】", NamedTextColor.YELLOW, TextDecoration.BOLD));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        List<String> lines = buildScoreboardLines(player);

        // 既存のエントリを一度クリアして新しい行を設定
        for (String entry : scoreboard.getEntries()) {
            if (obj.getScore(entry).isScoreSet()) {
                scoreboard.resetScores(entry);
            }
        }

        int score = lines.size();
        for (String line : lines) {
            obj.getScore(line).setScore(score--);
        }
    }

    private List<String> buildScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        List<Quest> acceptedQuests = questService.getAcceptedQuests(player);

        if (acceptedQuests.isEmpty()) {
            lines.add("§7(進行中のクエストなし)");
            return lines;
        }

        QuestProgress progress = questService.getProgress(player.getUniqueId());

        for (Quest quest : acceptedQuests) {
            String categoryTag = quest.getCategory().getPrefix();
            lines.add(categoryTag + " §f" + quest.getTitle());

            QuestEntry entry = progress.getEntry(quest.getId());

            for (QuestObjective obj : quest.getObjectives()) {
                int count;
                if (obj.getType() == ObjectiveType.COLLECT_ITEM && obj.getItemId() != null) {
                    count = questService.countItem(player, obj.getItemId());
                } else if (entry != null) {
                    count = entry.getCounter(obj.getProgressKey());
                } else {
                    count = 0;
                }

                String display = obj.getProgressDisplay(count, player);
                lines.add(display);
            }

            // クエスト完了可能かどうかの案内
            if (questService.checkCompletion(player, quest.getId())) {
                lines.add("  §a↳ " + quest.getNpcName() + "に報告可能！");
            }

            lines.add("§8 "); // 区切り空行
        }

        // 最後の空行をトリム
        if (!lines.isEmpty() && lines.get(lines.size() - 1).equals("§8 ")) {
            lines.remove(lines.size() - 1);
        }

        // 最大15行に収める
        if (lines.size() > 15) {
            lines = lines.subList(0, 15);
        }

        return lines;
    }
}
