package com.ruskserver.deepwither_V2.modules.revival;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.mob.region.MobRegionConfig;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerLevelProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RevivalManager implements Startable, Stoppable, Listener {

    private static final int REVIVE_RANGE = 3;
    private static final double REVIVE_RANGE_SQ = REVIVE_RANGE * REVIVE_RANGE;
    private static final int PROGRESS_NEEDED = 50;
    private static final long TIMEOUT_MS = 120_000;
    private static final double REVIVE_HP_RATIO = 0.2;

    private final Map<UUID, DownedPlayerData> downedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, RevivalSession> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> pendingDeathPenalty = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    private final VirtualHealthManager healthManager;
    private final PlayerDataRepository repository;
    private final PlayerManager playerManager;
    private final JavaPlugin plugin;
    private final MobRegionConfig regionConfig;
    private final Logger logger;

    private BukkitTask tickTask;

    @Inject
    public RevivalManager(VirtualHealthManager healthManager, PlayerDataRepository repository,
                          PlayerManager playerManager, JavaPlugin plugin, MobRegionConfig regionConfig) {
        this.healthManager = healthManager;
        this.repository = repository;
        this.playerManager = playerManager;
        this.plugin = plugin;
        this.regionConfig = regionConfig;
        this.logger = plugin.getLogger();
    }

    @Override
    public void start() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 2L);
        logger.info("[RevivalManager] 蘇生システムを開始しました");
    }

    @Override
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        downedPlayers.clear();
        sessions.clear();
        pendingDeathPenalty.clear();
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
    }

    public void enterDownState(Player player) {
        UUID uuid = player.getUniqueId();

        int expAtDeath = 0;
        var dataOpt = repository.get(uuid);
        if (dataOpt.isPresent()) {
            PlayerLevelProvider.LevelData levelData = dataOpt.get().get(PlayerLevelProvider.KEY);
            if (levelData != null) {
                expAtDeath = levelData.getExp();
            }
        }

        DownedPlayerData data = new DownedPlayerData(uuid, System.currentTimeMillis(),
                player.getLocation().clone(), expAtDeath);
        downedPlayers.put(uuid, data);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 7, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, PotionEffect.INFINITE_DURATION, 4, false, false));
        player.setGameMode(GameMode.ADVENTURE);
        player.setWalkSpeed(0.0f);
        player.setGlowing(true);
        player.setCollidable(false);
        player.setAllowFlight(false);
        player.setFlying(false);

        BossBar bar = Bukkit.createBossBar(
                "§c⚠ ダウン状態 §7- §e/suicide §7でリスポーン",
                BarColor.RED, BarStyle.SOLID
        );
        bar.addPlayer(player);
        bar.setVisible(true);
        bossBars.put(uuid, bar);

        player.sendMessage(Component.text("§c╔══════════════════════════════════╗"));
        player.sendMessage(Component.text("§c║ §c⚠ あなたはダウン状態です！           §c║"));
        player.sendMessage(Component.text("§c║ §e/suicide §7でリスポーン             §c║"));
        player.sendMessage(Component.text("§c║ §a味方が近くでスニーク長押しで蘇生       §c║"));
        player.sendMessage(Component.text("§c╚══════════════════════════════════╝"));
        player.sendMessage(Component.text("§8ヒント: 2分経過で自動的にリスポーンします"));
    }

    public void revive(Player target) {
        UUID targetId = target.getUniqueId();
        if (!downedPlayers.containsKey(targetId)) return;

        target.removePotionEffect(PotionEffectType.SLOWNESS);
        target.removePotionEffect(PotionEffectType.BLINDNESS);
        target.removePotionEffect(PotionEffectType.WEAKNESS);
        target.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        target.setGameMode(GameMode.SURVIVAL);
        target.setWalkSpeed(0.2f);
        target.setGlowing(false);
        target.setCollidable(true);

        double maxHp = healthManager.getMaxHealth(target);
        healthManager.heal(target, maxHp * REVIVE_HP_RATIO);

        downedPlayers.remove(targetId);
        BossBar bar = bossBars.remove(targetId);
        if (bar != null) {
            bar.removeAll();
        }

        target.showTitle(Title.title(
                Component.text("§a蘇生されました！"),
                Component.text("§7HPが " + (int) (maxHp * REVIVE_HP_RATIO) + " 回復しました")
        ));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        target.sendMessage(Component.text("§a>> 蘇生されました！"));
    }

    public void forceDeath(Player player) {
        UUID uuid = player.getUniqueId();
        if (!downedPlayers.containsKey(uuid)) return;

        pendingDeathPenalty.add(uuid);

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.setGameMode(GameMode.SURVIVAL);
        player.setWalkSpeed(0.2f);
        player.setGlowing(false);
        player.setCollidable(true);

        downedPlayers.remove(uuid);
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        RevivalSession session = sessions.remove(uuid);
        if (session != null) {
            session.bossBar.removeAll();
        }

        player.setHealth(0);
    }

    public boolean isDowned(Player player) {
        return downedPlayers.containsKey(player.getUniqueId());
    }

    public boolean hasDeathPenalty(UUID uuid) {
        return pendingDeathPenalty.contains(uuid);
    }

    public void consumeDeathPenalty(UUID uuid) {
        pendingDeathPenalty.remove(uuid);
    }

    private void tick() {
        long now = System.currentTimeMillis();

        for (DownedPlayerData data : downedPlayers.values()) {
            if (now - data.downedAt > TIMEOUT_MS) {
                Player player = Bukkit.getPlayer(data.playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text("§c>> 蘇生タイムアウト: 自動的にリスポーンします。"));
                    forceDeath(player);
                }
                continue;
            }

            Player target = Bukkit.getPlayer(data.playerId);
            if (target == null || !target.isOnline()) continue;

            boolean hasActiveSession = sessions.containsKey(data.playerId);
            BossBar bar = bossBars.get(data.playerId);
            if (bar != null) {
                bar.setVisible(!hasActiveSession);
                if (!hasActiveSession) {
                    long elapsed = now - data.downedAt;
                    long remaining = (TIMEOUT_MS - elapsed) / 1000;
                    bar.setTitle(String.format(
                            "§c⚠ ダウン状態 §7- §e/suicide §7でリスポーン | §c%d秒",
                            Math.max(0, remaining)
                    ));
                    double progress = (double) elapsed / TIMEOUT_MS;
                    bar.setProgress(Math.min(1.0, progress));
                }
            }

            List<Player> nearbyRevivers = target.getNearbyEntities(REVIVE_RANGE, REVIVE_RANGE, REVIVE_RANGE)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .filter(p -> !p.equals(target))
                    .filter(Player::isSneaking)
                    .filter(p -> !isDowned(p))
                    .filter(p -> p.getLocation().distanceSquared(target.getLocation()) <= REVIVE_RANGE_SQ)
                    .collect(Collectors.toList());

            Set<UUID> reviverIds = nearbyRevivers.stream().map(Player::getUniqueId).collect(Collectors.toSet());

            if (reviverIds.isEmpty()) {
                RevivalSession session = sessions.remove(data.playerId);
                if (session != null) {
                    session.bossBar.removeAll();
                }
                continue;
            }

            RevivalSession session = sessions.computeIfAbsent(data.playerId, k -> {
                RevivalSession s = new RevivalSession();
                s.bossBar = Bukkit.createBossBar(
                        "§a蘇生中... §70%",
                        BarColor.GREEN, BarStyle.SEGMENTED_20
                );
                s.bossBar.setVisible(true);
                return s;
            });

            session.reviverIds = reviverIds;

            session.bossBar.removeAll();
            for (Player reviver : nearbyRevivers) {
                session.bossBar.addPlayer(reviver);
            }
            session.bossBar.addPlayer(target);

            session.progress += reviverIds.size();
            int progressPercent = Math.min(100, session.progress * 100 / PROGRESS_NEEDED);
            session.bossBar.setTitle(String.format("§a蘇生中... §7%d%%", progressPercent));
            session.bossBar.setProgress(Math.min(1.0, (double) session.progress / PROGRESS_NEEDED));

            if (session.progress >= PROGRESS_NEEDED) {
                session.bossBar.removeAll();
                sessions.remove(data.playerId);
                revive(target);
                for (UUID revId : reviverIds) {
                    Player reviver = Bukkit.getPlayer(revId);
                    if (reviver != null && reviver.isOnline()) {
                        reviver.sendMessage(Component.text("§a>> ").append(
                                Component.text(target.getName(), NamedTextColor.GOLD)
                        ).append(Component.text(" の蘇生に成功しました！")));
                        reviver.playSound(reviver.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (downedPlayers.containsKey(uuid)) {
            downedPlayers.remove(uuid);
            BossBar bar = bossBars.remove(uuid);
            if (bar != null) bar.removeAll();
            RevivalSession session = sessions.remove(uuid);
            if (session != null) session.bossBar.removeAll();
        }
    }

    private static class DownedPlayerData {
        final UUID playerId;
        final long downedAt;
        final Location location;
        final int expAtDeath;

        DownedPlayerData(UUID playerId, long downedAt, Location location, int expAtDeath) {
            this.playerId = playerId;
            this.downedAt = downedAt;
            this.location = location;
            this.expAtDeath = expAtDeath;
        }
    }

    private static class RevivalSession {
        Set<UUID> reviverIds = new HashSet<>();
        int progress = 0;
        BossBar bossBar;
    }
}
