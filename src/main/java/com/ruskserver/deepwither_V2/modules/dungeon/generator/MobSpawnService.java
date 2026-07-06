package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.dungeon.modifier.DungeonModifierContext;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        return spawnMobs(world, mobPositions, mobId, mobType, DungeonModifierContext.none());
    }

    public List<CustomMob> spawnMobs(World world, List<BlockVector3> mobPositions, String mobId) {
        return spawnMobs(world, mobPositions, mobId, null, DungeonModifierContext.none());
    }

    public List<CustomMob> spawnMobs(World world, List<BlockVector3> mobPositions) {
        return spawnMobs(world, mobPositions, null, null, DungeonModifierContext.none());
    }

    public List<CustomMob> spawnMobs(World world, List<BlockVector3> mobPositions, String mobId, DungeonModifierContext ctx) {
        return spawnMobs(world, mobPositions, mobId, null, ctx);
    }

    public List<CustomMob> spawnMobs(World world, List<BlockVector3> mobPositions, String mobId, EntityType mobType, DungeonModifierContext ctx) {
        String effectiveMobId = (mobId != null && !mobId.isBlank()) ? mobId : DEFAULT_DUNGEON_MOB_ID;
        List<CustomMob> spawned = new ArrayList<>();

        int extraCount = ctx.isPresent() ? (int) Math.ceil(ctx.combinedMobCount() - 1.0) : 0;
        if (extraCount < 0) extraCount = 0;
        Random rng = new Random();

        for (BlockVector3 pos : mobPositions) {
            Location loc = new Location(world, pos.x() + 0.5, pos.y(), pos.z() + 0.5);
            CustomMob mob = mobType == null
                    ? customMobManager.spawnMob(effectiveMobId, loc)
                    : customMobManager.spawnMob(effectiveMobId, loc, mobType);
            if (mob != null && mob.getEntity() != null) {
                applyModifierToMob(mob, ctx);
                spawned.add(mob);
            }

            for (int i = 0; i < extraCount; i++) {
                Location offset = loc.clone().add(
                        (rng.nextDouble() - 0.5) * 3.0,
                        0,
                        (rng.nextDouble() - 0.5) * 3.0
                );
                CustomMob extra = mobType == null
                        ? customMobManager.spawnMob(effectiveMobId, offset)
                        : customMobManager.spawnMob(effectiveMobId, offset, mobType);
                if (extra != null && extra.getEntity() != null) {
                    applyModifierToMob(extra, ctx);
                    spawned.add(extra);
                }
            }
        }

        log.info("[MobSpawnService] Spawned " + spawned.size() + " dungeon mobs (" + effectiveMobId + ")");
        return spawned;
    }

    private void applyModifierToMob(CustomMob mob, DungeonModifierContext ctx) {
        if (!ctx.isPresent()) return;
        LivingEntity entity = mob.getEntity();
        if (entity == null) return;

        double atkMult = ctx.combinedMobAtk();
        double hpMult = ctx.combinedMobHp();

        if (atkMult != 1.0) {
            var attr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attr != null) {
                attr.setBaseValue(attr.getBaseValue() * atkMult);
            }
        }

        if (hpMult != 1.0) {
            double oldMax = entity.getMaxHealth();
            double newMax = oldMax * hpMult;
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMax);
            entity.setHealth(Math.min(entity.getHealth() * hpMult, newMax));
        }
    }
}
