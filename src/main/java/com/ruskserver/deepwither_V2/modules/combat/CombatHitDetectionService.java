package com.ruskserver.deepwither_V2.modules.combat;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.combat.shape.HitShape;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class CombatHitDetectionService {

    private final Random random = new Random();
    private final Set<UUID> debugPlayers = new HashSet<>();
    private final Map<UUID, Set<UUID>> recentlyHit = new HashMap<>();

    private static final double[] SWORD_ROTATIONS = {0.0, 180.0, 45.0, 135.0};

    public void toggleDebug(Player player) {
        if (debugPlayers.contains(player.getUniqueId())) {
            debugPlayers.remove(player.getUniqueId());
            player.sendMessage("§a[HitDetection] デバッグ表示を無効にしました。");
        } else {
            debugPlayers.add(player.getUniqueId());
            player.sendMessage("§a[HitDetection] デバッグ表示を有効にしました。");
        }
    }

    public List<LivingEntity> detectTargets(Player attacker, CombatWeaponType type) {
        WeaponHitProfile profile = WeaponHitProfile.from(type);
        if (profile == null) return List.of();

        Location origin = attacker.getEyeLocation().subtract(0, 0.4, 0);
        Vector direction = attacker.getEyeLocation().getDirection();

        double reach = profile.baseReach();
        World world = attacker.getWorld();

        double rotation = 0;
        if (profile.visualType() == HitShape.VisualType.SWORD) {
            rotation = SWORD_ROTATIONS[random.nextInt(SWORD_ROTATIONS.length)];
        }

        profile.shape().spawnSlashEffect(origin, direction, reach, profile.visualType(), rotation);

        if (debugPlayers.contains(attacker.getUniqueId())) {
            profile.shape().drawDebug(origin, direction, reach, rotation);
        }

        Collection<Entity> candidates = world.getNearbyEntities(origin, reach + 2, reach + 2, reach + 2);
        List<LivingEntity> hits = new ArrayList<>();

        for (Entity entity : candidates) {
            if (!(entity instanceof LivingEntity target) || entity.equals(attacker)) continue;
            if (entity instanceof org.bukkit.entity.ArmorStand || entity instanceof org.bukkit.entity.Hanging) continue;

            if (!profile.shape().isHit(origin, direction, target, reach, rotation)) continue;

            RayTraceResult ray = world.rayTraceBlocks(origin,
                    target.getLocation().add(0, target.getHeight() / 2, 0).toVector().subtract(origin.toVector()),
                    origin.distance(target.getLocation().add(0, target.getHeight() / 2, 0)),
                    FluidCollisionMode.NEVER, true);
            if (ray != null && ray.getHitBlock() != null) continue;

            hits.add(target);
        }

        if (hits.isEmpty()) return List.of();

        Set<UUID> hitSet = recentlyHit.computeIfAbsent(attacker.getUniqueId(), k -> new HashSet<>());

        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity victim : hits) {
            if (hitSet.contains(victim.getUniqueId())) continue;
            hitSet.add(victim.getUniqueId());
            result.add(victim);
        }

        recentlyHit.put(attacker.getUniqueId(), hitSet);
        // Clean up after 1 tick
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(CombatHitDetectionService.class),
                () -> {
                    Set<UUID> cleared = recentlyHit.remove(attacker.getUniqueId());
                    if (cleared != null) cleared.clear();
                }, 1L);

        result.sort(Comparator.comparingDouble(l -> l.getLocation().distanceSquared(attacker.getLocation())));
        return result;
    }
}
