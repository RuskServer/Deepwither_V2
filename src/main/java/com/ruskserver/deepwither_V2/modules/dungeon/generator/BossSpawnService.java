package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
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
 * {@link CustomMobManager} 経由でカスタムモブボスにも対応します。
 */
@Service
public class BossSpawnService {

    private final Logger log;
    private final CustomMobManager mobManager;

    @Inject
    public BossSpawnService(Logger log, CustomMobManager mobManager) {
        this.log = log;
        this.mobManager = mobManager;
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
        return spawnBoss(world, position, (EntityType) null);
    }

    /**
     * カスタムモブIDを指定してボスをスポーンさせます。
     * 指定されたモブIDが {@link CustomMobManager} に登録されている場合、
     * カスタムモブとしてスポーンします。未登録の場合はフォールバックとして
     * デフォルトボス（WITHER）をスポーンします。
     *
     * @param world    対象ワールド
     * @param position スポーン位置（ワールド座標）
     * @param mobId    カスタムモブID（null の場合はデフォルトボス）
     * @return スポーンしたエンティティ。スポーン失敗の場合は null
     */
    public Entity spawnBoss(World world, BlockVector3 position, String mobId) {
        if (world == null || position == null) {
            log.warning("[BossSpawnService] ワールドまたは位置が null");
            return null;
        }
        if (mobId == null || !mobManager.hasRegistration(mobId)) {
            return spawnBoss(world, position, (EntityType) null);
        }

        Location loc = new Location(world, position.x() + 0.5, position.y(), position.z() + 0.5);
        try {
            CustomMob boss = mobManager.spawnMob(mobId, loc, 1);
            if (boss != null) {
                log.info("[BossSpawnService] カスタムボススポーン: " + mobId
                        + " at (" + position.x() + ", " + position.y() + ", " + position.z() + ")");
                return boss.getEntity();
            }
        } catch (Exception e) {
            log.warning("[BossSpawnService] カスタムボススポーン失敗: " + mobId + " - " + e.getMessage());
        }
        return spawnBoss(world, position, (EntityType) null);
    }
}
