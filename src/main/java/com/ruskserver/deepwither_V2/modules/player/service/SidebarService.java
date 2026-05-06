package com.ruskserver.deepwither_V2.modules.player.service;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SidebarService implements Startable, Listener {

    private final ManaManager manaManager;
    private final VirtualHealthManager healthManager;
    private final Deepwither_V2 plugin;

    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    @Inject
    public SidebarService(ManaManager manaManager, VirtualHealthManager healthManager,
                          Deepwither_V2 plugin) {
        this.manaManager = manaManager;
        this.healthManager = healthManager;
        this.plugin = plugin;
    }

    @Override
    public void start() {
        // 初期化：現在オンラインのプレイヤーにスコアボードを設定
        for (Player player : Bukkit.getOnlinePlayers()) {
            setupScoreboard(player);
        }

        // 定期更新タスク (10 ticks = 0.5s ごと)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateSidebar(player);
            }
        }, 20L, 10L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        setupScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboards.remove(event.getPlayer().getUniqueId());
    }

    private void setupScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(scoreboard);
        scoreboards.put(player.getUniqueId(), scoreboard);
        
        Objective objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY,
                Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private void updateSidebar(Player player) {
        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("sidebar");
        if (objective == null) return;

        double currentHp = healthManager.getHealth(player);
        double maxHp = healthManager.getMaxHealth(player);
        double currentMana = manaManager.getMana(player);
        double maxMana = getMaxManaSafe(player);
        
        // シンプルな更新：既存のスコアをリセットして再セット
        // ※ チカつきを抑えるには Team を使うべきだが、まずは要件を満たすシンプルな実装とする
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int score = 2;
        setLine(objective, "§f HP: §c" + (int)currentHp + " / " + (int)maxHp, score--);
        setLine(objective, "§f マナ: §b" + (int)currentMana + " / " + (int)maxMana, score--);
    }

    private double getMaxManaSafe(Player player) {
        double max = manaManager.getMaxMana(player);
        return max > 0 ? max : 1.0;
    }

    private void setLine(Objective objective, String text, int score) {
        objective.getScore(text).setScore(score);
    }
}
