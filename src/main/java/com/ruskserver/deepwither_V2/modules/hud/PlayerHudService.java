package com.ruskserver.deepwither_V2.modules.hud;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.combat.health.event.VirtualHealthChangeEvent;
import com.ruskserver.deepwither_V2.modules.hud.EoaHudLayout.HudSnapshot;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BetterHUD を置き換える自前 HUD 管理サービス。
 * <p>
 * プレイヤーごとに HUD 専用の透明 BossBar ({@link BossBar.Color#PINK}) を割り当て、
 * 仮想HP・バリア・マジックシールド(将来拡張用フック)・マナを {@code eoa:hud} フォントで描画する。
 * ActionBar は詠唱・対話 UI で使用されるため一切占有しない。
 */
@Service
public class PlayerHudService implements Startable, Stoppable, Listener {

    private static final long UPDATE_INTERVAL_TICKS = 2L;

    private final Deepwither_V2 plugin;
    private final VirtualHealthManager healthManager;
    private final ManaManager manaManager;

    private final Map<UUID, PlayerHudState> states = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    @Inject
    public PlayerHudService(
            Deepwither_V2 plugin,
            VirtualHealthManager healthManager,
            ManaManager manaManager
    ) {
        this.plugin = plugin;
        this.healthManager = healthManager;
        this.manaManager = manaManager;
    }

    @Override
    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            attach(player);
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);
    }

    @Override
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (Map.Entry<UUID, PlayerHudState> entry : states.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && entry.getValue().visible) {
                player.hideBossBar(entry.getValue().bossBar);
            }
        }
        states.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        attach(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        detach(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                refresh(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVirtualHealthChange(VirtualHealthChangeEvent event) {
        if (event.getEntity() instanceof Player player && player.isOnline()) {
            refresh(player);
        }
    }

    /** 即時更新が必要な処理（マナ消費・バリア変化など）から呼び出せる公開メソッド */
    public void refresh(Player player) {
        PlayerHudState state = states.get(player.getUniqueId());
        if (state == null) return;

        boolean shouldShow = player.getGameMode() != GameMode.SPECTATOR;
        if (!shouldShow) {
            if (state.visible) {
                player.hideBossBar(state.bossBar);
                state.visible = false;
            }
            return;
        }

        if (!state.visible) {
            player.showBossBar(state.bossBar);
            state.visible = true;
        }

        HudSnapshot snapshot = HudSnapshot.of(
                healthManager.getHealth(player),
                healthManager.getMaxHealth(player),
                manaManager.getMana(player),
                manaManager.getMaxMana(player),
                healthManager.getBarrier(player),
                0.0,
                0.0
        );
        if (!snapshot.equals(state.lastSnapshot)) {
            state.lastSnapshot = snapshot;
            state.bossBar.name(EoaHudRenderer.render(snapshot));
        }
    }

    private void attach(Player player) {
        PlayerHudState state = states.computeIfAbsent(player.getUniqueId(), id -> new PlayerHudState(
                BossBar.bossBar(Component.empty(), 0.0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS)
        ));
        refresh(player);
        if (!state.visible && player.getGameMode() != GameMode.SPECTATOR) {
            player.showBossBar(state.bossBar);
            state.visible = true;
        }
    }

    private void detach(Player player) {
        PlayerHudState state = states.remove(player.getUniqueId());
        if (state != null && state.visible) {
            player.hideBossBar(state.bossBar);
        }
    }

    private void tickAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private static final class PlayerHudState {
        final BossBar bossBar;
        boolean visible;
        HudSnapshot lastSnapshot;

        PlayerHudState(BossBar bossBar) {
            this.bossBar = bossBar;
        }
    }
}
