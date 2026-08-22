package com.ruskserver.deepwither_V2.modules.quest.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.mob.event.CustomMobDeathEvent;
import com.ruskserver.deepwither_V2.modules.quest.service.QuestScoreboardService;
import com.ruskserver.deepwither_V2.modules.quest.service.QuestService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@Component
public class QuestEventListener implements Listener {

    private final QuestService questService;
    private final QuestScoreboardService scoreboardService;

    @Inject
    public QuestEventListener(QuestService questService, QuestScoreboardService scoreboardService) {
        this.questService = questService;
        this.scoreboardService = scoreboardService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomMobDeath(CustomMobDeathEvent event) {
        Player killer = event.getKiller();
        if (killer == null) return;

        String mobId = event.getMobId();
        questService.incrementProgress(killer, "kill_" + mobId, 1);
        scoreboardService.updateScoreboard(killer);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scoreboardService.updateScoreboard(event.getPlayer());
    }
}
