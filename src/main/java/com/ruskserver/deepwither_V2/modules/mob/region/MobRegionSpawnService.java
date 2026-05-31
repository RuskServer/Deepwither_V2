package com.ruskserver.deepwither_V2.modules.mob.region;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Regionごとにカスタムモブのランダムスポーンを定期実行するサービス。
 * <p>
 * <ul>
 *   <li>セーフゾーンのRegionはスポーンをスキップします。</li>
 *   <li>Region内にプレイヤーが1人もいない場合はスポーンしません。</li>
 *   <li>Region内のアクティブモブ数が {@code maxMobsPerRegion} に達している場合はスポーンしません。</li>
 *   <li>スポーン位置はRegion内のランダムなプレイヤーを中心に
 *       {@value MIN_SPAWN_DIST}〜{@value MAX_SPAWN_DIST} ブロックの範囲で選択します。</li>
 * </ul>
 */
@Service
public class MobRegionSpawnService implements Startable, Stoppable {

    private final JavaPlugin plugin;
    private final MobRegionConfig regionConfig;
    private final CustomMobManager mobManager;
    private final Logger log;
    private final Random random = new Random();

    /** プレイヤーから最低この距離以上離れた場所にスポーンする（ブロック） */
    private static final int MIN_SPAWN_DIST = 24;
    /** プレイヤーからこの距離以内にスポーンする（ブロック） */
    private static final int MAX_SPAWN_DIST = 64;
    /** スポーン座標の候補を何回試みるか */
    private static final int MAX_SPAWN_ATTEMPTS = 10;
    /** プレイヤーのY座標から上下スキャンする範囲（ブロック） */
    private static final int SCAN_RANGE = 16;
    /** セーフゾーン内でこの時間（ms）経過するとモブをデスポーン */
    private static final long SAFE_ZONE_DESPAWN_MS = 5_000L;

    /** プレイヤーがセーフゾーンに入った時刻（ UUID → System.currentTimeMillis() ） */
    private final Map<UUID, Long> safeZoneEntryTime = new HashMap<>();

    /** 各Regionの定期スポーンタスク（stop時にキャンセルするために保持） */
    private final List<BukkitTask> spawnTasks = new ArrayList<>();

    @Inject
    public MobRegionSpawnService(JavaPlugin plugin, MobRegionConfig regionConfig, CustomMobManager mobManager) {
        this.plugin = plugin;
        this.regionConfig = regionConfig;
        this.mobManager = mobManager;
        this.log = plugin.getLogger();
    }

    @Override
    public void start() {
        scheduleAll();
        startSafeZoneCleaner();
        log.info("[MobRegionSpawnService] スポーンスケジューラーを起動しました。");
    }

    @Override
    public void stop() {
        for (BukkitTask task : spawnTasks) {
            task.cancel();
        }
        spawnTasks.clear();
        safeZoneEntryTime.clear();
        log.info("[MobRegionSpawnService] 全スポーンタスクを停止しました。");
    }

    /**
     * 全Regionのスポーンタスクを再スケジュールします。
     * {@link MobRegionConfig#reload()} の後に呼び出してください。
     */
    public void reschedule() {
        stop();
        scheduleAll();
    }

    // --- 内部処理 ---

    private void scheduleAll() {
        for (MobRegion region : regionConfig.getRegions()) {
            if (region.isSafeZone()) {
                log.info("[MobRegionSpawnService] Region '" + region.name() + "' はセーフゾーンのためスキップします。");
                continue;
            }
            if (region.spawnTable().isEmpty()) {
                log.warning("[MobRegionSpawnService] Region '" + region.name() + "' のスポーンテーブルが空です。スキップします。");
                continue;
            }
            if (region.spawnIntervalTicks() <= 0) {
                log.warning("[MobRegionSpawnService] Region '" + region.name() + "' の spawn-interval-ticks が0以下です。スキップします。");
                continue;
            }

            long interval = region.spawnIntervalTicks();
            BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                    plugin,
                    () -> attemptSpawn(region),
                    interval,
                    interval
            );
            spawnTasks.add(task);
            log.info("[MobRegionSpawnService] Region '" + region.name()
                    + "' (WG: " + region.wgRegion().getId() + ")"
                    + " のスポーンタスクを登録しました。"
                    + " (interval=" + interval + "ticks, maxMobs=" + region.maxMobsPerRegion() + ")");
        }
    }

    /**
     * 指定Regionに対してスポーンを1回試みます。
     */
    private void attemptSpawn(MobRegion region) {
        // Region内にプレイヤーがいない場合はスポーンしない
        List<org.bukkit.entity.Player> playersInRegion = getPlayersInRegion(region);
        if (playersInRegion.isEmpty()) return;

        // 現在のRegion内アクティブモブ数をカウント
        long currentCount = countActiveMobsIn(region);
        if (currentCount >= region.maxMobsPerRegion()) {
            return;
        }

        // Weighted Randomでモブを選択
        String selectedMobId = weightedRandom(region);
        if (selectedMobId == null) return;

        // 登録されているか確認
        if (!mobManager.hasRegistration(selectedMobId)) {
            log.warning("[MobRegionSpawnService] Region '" + region.name()
                    + "' のスポーンテーブルに未登録のモブID: " + selectedMobId);
            return;
        }

        // プレイヤー周辺のランダムなスポーン座標を決定
        org.bukkit.entity.Player target = playersInRegion.get(random.nextInt(playersInRegion.size()));
        Location spawnLoc = nearbyPlayerSurfaceLocation(region, target);
        if (spawnLoc == null) return;

        mobManager.spawnMob(selectedMobId, spawnLoc);
    }

    /**
     * Region内にいるプレイヤーのリストを返します。
     * セーフゾーン内にいるプレイヤーは除外します。
     */
    private List<Player> getPlayersInRegion(MobRegion region) {
        List<MobRegion> safeZones = getSafeZones();
        return region.world().getPlayers().stream()
                .filter(p -> !p.isDead() && region.contains(p.getLocation()))
                .filter(p -> p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR)
                .filter(p -> safeZones.stream().noneMatch(sz -> sz.contains(p.getLocation())))
                .toList();
    }

    /** セーフゾーンのRegionリストを返します。 */
    private List<MobRegion> getSafeZones() {
        return regionConfig.getRegions().stream()
                .filter(MobRegion::isSafeZone)
                .toList();
    }

    /**
     * 現在アクティブなカスタムモブのうち、このRegion内にいる数を返します。
     */
    private long countActiveMobsIn(MobRegion region) {
        return mobManager.getActiveMobs().stream()
                .map(CustomMob::getLocation)
                .filter(region::contains)
                .count();
    }

    // =========================================================
    // セーフゾーンクリーナー
    // =========================================================

    /**
     * セーフゾーンクリーナータスクを起動します。
     * 毎科（1回）全プレイヤーのセーフゾーン在室を確認し、
     * {@value SAFE_ZONE_DESPAWN_MS}ms 経過したプレイヤーのセーフゾーン内のモブを全削除します。
     */
    private void startSafeZoneCleaner() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            List<MobRegion> safeZones = getSafeZones();
            if (safeZones.isEmpty()) return;

            long now = System.currentTimeMillis();
            boolean anyDespawn = false;

            // 既にオフラインになったプレイヤーのエントリを削除
            safeZoneEntryTime.keySet().removeIf(id ->
                    plugin.getServer().getPlayer(id) == null);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                boolean inSafeZone = safeZones.stream()
                        .anyMatch(sz -> sz.contains(player.getLocation()));

                if (inSafeZone) {
                    // 初めて入った時刻を記録
                    safeZoneEntryTime.putIfAbsent(player.getUniqueId(), now);
                    long elapsed = now - safeZoneEntryTime.get(player.getUniqueId());

                    if (elapsed >= SAFE_ZONE_DESPAWN_MS) {
                        // 5秒経過→デスポーンフラグを立てる
                        anyDespawn = true;
                        // 次回の事前百秒間は冗長に実行されないよう、穎削する
                        safeZoneEntryTime.put(player.getUniqueId(), now);
                    }
                } else {
                    // 小退したらリセット
                    safeZoneEntryTime.remove(player.getUniqueId());
                }
            }

            // 1人でもデスポーン対象がいればセーフゾーン内のモブを一收
            if (anyDespawn) {
                safeZones.forEach(sz ->
                        mobManager.despawnMobsIn(sz::contains));
            }
        }, 20L, 20L);
    }

    /**
     * スポーンテーブルからWeighted Randomでモブを1つ選択します。
     *
     * @return 選ばれたモブID。スポーンテーブルが空または合計重みが0の場合はnull
     */
    private String weightedRandom(MobRegion region) {
        int totalWeight = region.getTotalWeight();
        if (totalWeight <= 0) return null;

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (SpawnEntry entry : region.spawnTable()) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry.mobId();
            }
        }
        return region.spawnTable().get(region.spawnTable().size() - 1).mobId();
    }

    /**
     * 対象プレイヤーの周辺（{@value MIN_SPAWN_DIST}〜{@value MAX_SPAWN_DIST} ブロック）で
     * かつ Region 内にある、プレイヤーと近いY座標のスポーン座標を返します。
     * <p>
     * Y座標はプレイヤーのYを基準に上下 {@value SCAN_RANGE} ブロックをスキャンし、
     * 「足場が固体ブロック & 上2マスが空気」の最初の場所を選びます。
     * これにより地下・洞窟内でもプレイヤーと同じ高度帯に湿きます。
     *
     * @param region スポーン対象Region
     * @param player スポーン基準プレイヤー
     * @return スポーン可能な座標、または取得できなかった場合はnull
     */
    private Location nearbyPlayerSurfaceLocation(MobRegion region, org.bukkit.entity.Player player) {
        BlockVector3 min = region.wgRegion().getMinimumPoint();
        BlockVector3 max = region.wgRegion().getMaximumPoint();
        World world = region.world();

        int range = MAX_SPAWN_DIST - MIN_SPAWN_DIST;
        int playerY = (int) player.getLocation().getY();

        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            // プレイヤーを中心に MIN〜MAX ブロックのランダム方向オフセット
            double angle = random.nextDouble() * 2 * Math.PI;
            int dist = MIN_SPAWN_DIST + random.nextInt(range + 1);
            int x = (int) (player.getLocation().getX() + dist * Math.cos(angle));
            int z = (int) (player.getLocation().getZ() + dist * Math.sin(angle));

            // Region の XZ 境界内に収まっているか確認
            if (x < min.x() || x > max.x() || z < min.z() || z > max.z()) continue;

            // プレイヤーのYから上下にSCAN_RANGEブロックスキャンし、立てる場所を探す
            Integer spawnY = findStandableY(world, x, z, playerY, min.y(), max.y());
            if (spawnY == null) continue;

            return new Location(world, x + 0.5, spawnY, z + 0.5);
        }

        return null;  // MAX_SPAWN_ATTEMPTS 回試みても見つからなかった
    }

    /**
     * 指定 XZ 座標の、{@code centerY} を基準に上下 {@value SCAN_RANGE} ブロックをスキャンし、
     * 「足場が固体ブロック & 上2マスが空気」となる最初の Y 座標（エンティティが立つY）を返します。
     * centerY に最も近い場所を優先するため、dy=0→±1→±2... の順で探索します。
     *
     * @return エンティティが立つY座標、見つからなければ null
     */
    private Integer findStandableY(World world, int x, int z, int centerY, int minY, int maxY) {
        for (int dy = 0; dy <= SCAN_RANGE; dy++) {
            for (int sign : (dy == 0 ? new int[]{0} : new int[]{1, -1})) {
                int y = centerY + dy * sign;
                if (y < minY || y > maxY) continue;

                // y-1が固体ブロック（足場）で、yとy+1が空気（立てる）か確認
                if (world.getBlockAt(x, y - 1, z).getType().isSolid()
                        && !world.getBlockAt(x, y, z).getType().isSolid()
                        && !world.getBlockAt(x, y + 1, z).getType().isSolid()) {
                    return y;
                }
            }
        }
        return null;
    }
}
