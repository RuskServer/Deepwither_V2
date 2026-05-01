package com.ruskserver.deepwither_V2.modules.mob.region;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Regionごとにカスタムモブのランダムスポーンを定期実行するサービス。
 * <p>
 * <ul>
 *   <li>セーフゾーンのRegionはスポーンをスキップします。</li>
 *   <li>Region内のアクティブモブ数が {@code maxMobsPerRegion} に達している場合はスポーンしません。</li>
 *   <li>スポーン位置は WorldGuard Region の XZ 範囲内のランダム座標に
 *       {@link World#getHighestBlockYAt(int, int)} を用いて地面を算出します。</li>
 * </ul>
 */
@Service
public class MobRegionSpawnService implements Startable, Stoppable {

    private final JavaPlugin plugin;
    private final MobRegionConfig regionConfig;
    private final CustomMobManager mobManager;
    private final Logger log;
    private final Random random = new Random();

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
        log.info("[MobRegionSpawnService] スポーンスケジューラーを起動しました。");
    }

    @Override
    public void stop() {
        for (BukkitTask task : spawnTasks) {
            task.cancel();
        }
        spawnTasks.clear();
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

        // ランダムなスポーン座標を決定
        Location spawnLoc = randomSurfaceLocation(region);
        if (spawnLoc == null) return;

        mobManager.spawnMob(selectedMobId, spawnLoc);
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
     * WorldGuard Region の XZ 範囲内のランダムな地表座標を返します。
     * {@link com.sk89q.worldguard.protection.regions.ProtectedRegion#getMinimumPoint()} /
     * {@code getMaximumPoint()} でバウンディングボックスを取得し、その内側でランダムに選択します。
     * <p>
     * 地表の Y が Region の Y 範囲外の場合は null を返してスポーンをスキップします。
     *
     * @return スポーン可能な座標、または取得できなかった場合はnull
     */
    private Location randomSurfaceLocation(MobRegion region) {
        BlockVector3 min = region.wgRegion().getMinimumPoint();
        BlockVector3 max = region.wgRegion().getMaximumPoint();

        int rangeX = max.x() - min.x();
        int rangeZ = max.z() - min.z();

        // 範囲が0の場合（1ブロック幅）でも動作するよう保護
        int x = min.x() + (rangeX > 0 ? random.nextInt(rangeX + 1) : 0);
        int z = min.z() + (rangeZ > 0 ? random.nextInt(rangeZ + 1) : 0);

        World world = region.world();
        int surfaceY = world.getHighestBlockYAt(x, z);

        // 地表が Region の Y 範囲内にあるか確認
        if (surfaceY < min.y() || surfaceY > max.y()) {
            return null;
        }

        // 地面の1ブロック上（エンティティが立つ位置）をスポーン座標とする
        return new Location(world, x + 0.5, surfaceY + 1, z + 0.5);
    }
}
