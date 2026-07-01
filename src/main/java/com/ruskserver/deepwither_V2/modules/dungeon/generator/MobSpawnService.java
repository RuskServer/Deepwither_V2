package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class MobSpawnService {

    private static final String DEFAULT_DUNGEON_MOB_ID = "ghoul";

    private final CustomMobManager customMobManager;
    private final Logger log;

    @Inject
    public MobSpawnService(CustomMobManager customMobManager, Logger log) {
        this.customMobManager = customMobManager;
        this.log = log;
    }

    public List<CustomMob> spawnMobs(World world, List<BlockVector3> mobPositions, String mobId, EntityType mobType) {
        String effectiveMobId = (mobId != null && !mobId.isBlank()) ? mobId : DEFAULT_DUNGEON_MOB_ID;
        List<CustomMob> spawned = new ArrayList<>();
        for (BlockVector3 pos : mobPositions) {
            Location loc = new Location(world, pos.x() + 0.5, pos.y(), pos.z() + 0.5);
            CustomMob mob = mobType == null
                    ? customMobManager.spawnMob(effectiveMobId, loc)
                    : customMobManager.spawnMob(effectiveMobId, loc, mobType);
            if (mob != null && mob.getEntity() != null) {
                mob.getEntity().setRemoveWhenFarAway(false);
                spawned.add(mob);
                log.fine("[MobSpawnService] Spawned " + effectiveMobId + " at ("
                        + pos.x() + ", " + pos.y() + ", " + pos.z() + ")");
            } else {
                log.warning("[MobSpawnService] Failed to spawn " + effectiveMobId + " at ("
                        + pos.x() + ", " + pos.y() + ", " + pos.z() + ")");
            }
        }

        log.info("[MobSpawnService] Spawned " + spawned.size() + " dungeon mobs (" + effectiveMobId + ")");
        return spawned;
    }

    public List<CustomMob> spawnMobs(World world, List<BlockVector3> mobPositions, String mobId) {
        return spawnMobs(world, mobPositions, mobId, null);
    }

    public List<CustomMob> spawnMobs(World world, List<BlockVector3> mobPositions) {
        return spawnMobs(world, mobPositions, null, null);
    }
}
