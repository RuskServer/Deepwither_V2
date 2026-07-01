package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.logging.Logger;

/**
 * ダンジョン内のボススポーンを管理するサービス。
 * <p>
 * REDSTONE_BLOCK マーカーの位置にボスエンティティをスポーンさせます。
 */
@Service
public class BossSpawnService {

    private final Logger log;

    @Inject
    public BossSpawnService(Logger log) {
        this.log = log;
    }

    /**
     * 指定されたワールドにボスをスポーンさせます。
     *
     * @param world     対象ワールド
     * @param position  スポーン位置（ワールド座標）
     * @param bossType  スポーンさせるボスのエンティティタイプ（null の場合は WITHER）
     * @return スポーンしたエンティティ。スポーン失敗場合は null
     */
    public Entity spawnBoss(World world, BlockVector3 position, EntityType bossType) {
        if (bossType == null) {
            bossType = EntityType.WITHER;
        }

        if (world == null || position == null) {
            log.warning("[BossSpawnService] ワールドまたは位置が null");
            return null;
        }

        Location loc = new Location(world, position.x() + 0.5, position.y(), position.z() + 0.5);
        try {
            Entity boss = world.spawnEntity(loc, bossType);
            log.info("[BossSpawnService] ボススポーン: " + bossType.name()
                    + " at (" + position.x() + ", " + position.y() + ", " + position.z() + ")");
            return boss;
        } catch (Exception e) {
            log.warning("[BossSpawnService] ボススポーン失敗: " + bossType.name() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * デフォルトのボス（WITHER）をスポーンさせます。
     */
    public Entity spawnBoss(World world, BlockVector3 position) {
        return spawnBoss(world, position, null);
    }
}
