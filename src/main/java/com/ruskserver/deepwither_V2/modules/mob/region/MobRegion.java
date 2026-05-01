package com.ruskserver.deepwither_V2.modules.mob.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

/**
 * モブがスポーンする地理的な範囲を定義するデータクラス。
 * <p>
 * 座標範囲は WorldGuard の {@link ProtectedRegion} を参照します。
 * config.yml の {@code worldguard-region} キーで指定した WorldGuard Region名に対応します。
 *
 * @param name               Region名（config.yml のキーと一致）
 * @param isSafeZone         true の場合、このRegionではモブのスポーンが行われません
 * @param world              対象ワールド
 * @param wgRegion           WorldGuard の ProtectedRegion
 * @param spawnTable         このRegionに出現するモブのスポーンテーブル
 * @param spawnIntervalTicks スポーン試行を行うtick間隔
 * @param maxMobsPerRegion   このRegion内に同時に存在できる最大モブ数
 */
public record MobRegion(
        String name,
        boolean isSafeZone,
        World world,
        ProtectedRegion wgRegion,
        List<SpawnEntry> spawnTable,
        int spawnIntervalTicks,
        int maxMobsPerRegion
) {
    /**
     * 指定座標がこのRegion内にあるか判定します。
     * 座標判定には WorldGuard の {@link ProtectedRegion#contains(BlockVector3)} を使用します。
     */
    public boolean contains(Location loc) {
        if (!world.equals(loc.getWorld())) return false;
        return wgRegion.contains(
                BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())
        );
    }

    /**
     * スポーンテーブルの合計重みを返します。
     */
    public int getTotalWeight() {
        return spawnTable.stream().mapToInt(SpawnEntry::weight).sum();
    }
}
