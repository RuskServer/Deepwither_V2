package com.ruskserver.deepwither_V2.modules.combat.feedback;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

@Service
public class DamageImpactResolver {

    private static final double SURFACE_OFFSET = 0.06;

    public Location resolve(DamageContext context) {
        LivingEntity defender = context.getDefender();
        World world = defender.getWorld();
        Location requested = context.getImpactLocation();

        if (!isUsable(requested, world)) {
            LivingEntity attacker = context.getAttacker();
            if (attacker != null && attacker.getWorld().equals(world)) {
                requested = attacker.getEyeLocation();
            }
        }

        BoundingBox box = defender.getBoundingBox();
        Vector center = box.getCenter();
        if (!isUsable(requested, world)) {
            return center.toLocation(world).add(0.0, defender.getHeight() * 0.05, 0.0);
        }

        Vector requestedPoint = requested.toVector();
        Vector surface = box.contains(requestedPoint)
                ? projectInsidePointToSurface(box, center, requestedPoint)
                : clampToBox(box, requestedPoint);

        Vector outward = requestedPoint.clone().subtract(center);
        if (outward.lengthSquared() < 1.0E-6) {
            outward.setY(1.0);
        } else {
            outward.normalize();
        }
        return surface.toLocation(world).add(outward.multiply(SURFACE_OFFSET));
    }

    private Vector projectInsidePointToSurface(BoundingBox box, Vector center, Vector point) {
        Vector direction = point.clone().subtract(center);
        if (direction.lengthSquared() < 1.0E-6) {
            return new Vector(center.getX(), box.getMaxY(), center.getZ());
        }

        double scaleX = boundaryScale(direction.getX(), box.getWidthX() * 0.5);
        double scaleY = boundaryScale(direction.getY(), box.getHeight() * 0.5);
        double scaleZ = boundaryScale(direction.getZ(), box.getWidthZ() * 0.5);
        double scale = Math.min(scaleX, Math.min(scaleY, scaleZ));
        return center.clone().add(direction.multiply(scale));
    }

    private double boundaryScale(double component, double halfExtent) {
        if (Math.abs(component) < 1.0E-9) {
            return Double.POSITIVE_INFINITY;
        }
        return halfExtent / Math.abs(component);
    }

    private Vector clampToBox(BoundingBox box, Vector point) {
        return new Vector(
                Math.max(box.getMinX(), Math.min(box.getMaxX(), point.getX())),
                Math.max(box.getMinY(), Math.min(box.getMaxY(), point.getY())),
                Math.max(box.getMinZ(), Math.min(box.getMaxZ(), point.getZ()))
        );
    }

    private boolean isUsable(Location location, World expectedWorld) {
        return location != null
                && location.getWorld() != null
                && location.getWorld().equals(expectedWorld)
                && Double.isFinite(location.getX())
                && Double.isFinite(location.getY())
                && Double.isFinite(location.getZ());
    }
}
