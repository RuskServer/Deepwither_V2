package com.ruskserver.deepwither_V2.modules.player.service;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

/**
 * サイドバー（スコアボード）をプレイヤーごとに管理するサービス。
 *
 * <p>チカつき防止のため Team.prefix() を使い、エントリのスコアは固定したまま
 * テキストだけを毎 tick 更新する方式を採用しています。
 *
 * <p>表示レイアウト（上から）:
 * <pre>
 *  ─ Echoes of Aether ─  ← タイトル (Objective displayName)
 *  §c❤ 25/100            ← HP行
 *  §9⭐ 50/100           ← マナ行
 * </pre>
 */
@Service
public class SidebarService implements Startable, Listener {

    // --- 表示テキスト定数 ---
    private static final Component TITLE = Component.text("Echoes of Aether")
            .color(TextColor.color(0xE8C96F))
            .decoration(TextDecoration.BOLD, true);

    // Team エントリとして使う一意な文字列（色コード差しで重複しない）
    private static final String ENTRY_HP   = "§c";
    private static final String ENTRY_MANA = "§9";

    private static final int SCORE_HP   = 2;
    private static final int SCORE_MANA = 1;

    private static final String TEAM_HP   = "dw_hp";
    private static final String TEAM_MANA = "dw_mana";

    // --- 依存 ---
    private final ManaManager manaManager;
    private final VirtualHealthManager healthManager;
    private final Deepwither_V2 plugin;

    /** プレイヤーごとのスコアボード保持 */
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            setupScoreboard(player);
        }

        // 0.5秒ごとに更新
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

    // =========================================================
    // セットアップ
    // =========================================================

    /**
     * プレイヤー専用のスコアボードを初期化します。
     * Team のエントリとスコアはここで固定し、以後は prefix のみ更新します。
     */
    private void setupScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        // Objective
        Objective objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, TITLE);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // HP 行
        Team hpTeam = scoreboard.registerNewTeam(TEAM_HP);
        hpTeam.addEntry(ENTRY_HP);
        objective.getScore(ENTRY_HP).setScore(SCORE_HP);

        // マナ行
        Team manaTeam = scoreboard.registerNewTeam(TEAM_MANA);
        manaTeam.addEntry(ENTRY_MANA);
        objective.getScore(ENTRY_MANA).setScore(SCORE_MANA);

        player.setScoreboard(scoreboard);
        scoreboards.put(player.getUniqueId(), scoreboard);

        // 初回の値をすぐ反映
        updateSidebar(player);
    }

    // =========================================================
    // 更新
    // =========================================================

    /**
     * HP・マナの値だけを Team.prefix() で更新します。
     * エントリを削除・再追加しないためチカつきが発生しません。
     */
    private void updateSidebar(Player player) {
        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        double hp      = healthManager.getHealth(player);
        double maxHp   = healthManager.getMaxHealth(player);
        double mana    = manaManager.getMana(player);
        double maxMana = Math.max(manaManager.getMaxMana(player), 1.0);

        Team hpTeam   = scoreboard.getTeam(TEAM_HP);
        Team manaTeam = scoreboard.getTeam(TEAM_MANA);
        if (hpTeam == null || manaTeam == null) return;

        hpTeam.prefix(buildHpLine(hp, maxHp));
        manaTeam.prefix(buildManaLine(mana, maxMana));
    }

    // =========================================================
    // 行フォーマット
    // =========================================================

    /**
     * HP行: §c❤ 25/100
     */
    private Component buildHpLine(double current, double max) {
        return Component.text("❤ ")
                .color(NamedTextColor.RED)
                .append(Component.text((int) current)
                        .color(NamedTextColor.RED))
                .append(Component.text("/")
                        .color(NamedTextColor.DARK_GRAY))
                .append(Component.text((int) max)
                        .color(NamedTextColor.DARK_RED));
    }

    /**
     * マナ行: §9⭐ 50/100
     */
    private Component buildManaLine(double current, double max) {
        return Component.text("⭐ ")
                .color(NamedTextColor.BLUE)
                .append(Component.text((int) current)
                        .color(NamedTextColor.AQUA))
                .append(Component.text("/")
                        .color(NamedTextColor.DARK_GRAY))
                .append(Component.text((int) max)
                        .color(NamedTextColor.DARK_AQUA));
    }
}
