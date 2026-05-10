package com.ruskserver.deepwither_V2.modules.combat;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CombatHitDetectionService {

    public List<LivingEntity> detectTargets(Player attacker, CombatWeaponType type) {
        WeaponProfile profile = WeaponProfile.from(type);
        Location eye = attacker.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        World world = attacker.getWorld();

        Set<Entity> unique = new HashSet<>();
        List<LivingEntity> result = new ArrayList<>();
        int samples = 4;

        for (int i = 0; i < samples; i++) {
            double angle = ((double) i / (samples - 1) - 0.5) * profile.angleRadians;
            Vector rotated = rotateY(forward, angle);
            RayTraceResult ray = world.rayTrace(eye, rotated, profile.range, FluidCollisionMode.NEVER, true, profile.radius,
                    entity -> entity instanceof LivingEntity && entity != attacker);
            if (ray == null || ray.getHitEntity() == null) {
                continue;
            }
            Entity hit = ray.getHitEntity();
            if (unique.add(hit)) {
                result.add((LivingEntity) hit);
            }
        }

        result.sort(Comparator.comparingDouble(l -> l.getLocation().distanceSquared(attacker.getLocation())));
        return result;
    }

    private Vector rotateY(Vector vector, double rad) {
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z).normalize();
    }

    private record WeaponProfile(double range, double radius, double angleRadians) {
        private static WeaponProfile from(CombatWeaponType type) {
            return switch (type) {
                case HEAVY -> new WeaponProfile(2.8, 0.8, Math.toRadians(55));
                case SLASH -> new WeaponProfile(3.4, 0.9, Math.toRadians(95));
                case THRUST -> new WeaponProfile(4.2, 0.45, Math.toRadians(16));
            };
        }
    }
}
