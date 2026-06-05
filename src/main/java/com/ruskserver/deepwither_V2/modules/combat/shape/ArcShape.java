package com.ruskserver.deepwither_V2.modules.combat.shape;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class ArcShape implements HitShape {

    private final double angleDegrees;
    private final double thickness;

    public ArcShape(double angleDegrees, double thickness) {
        this.angleDegrees = angleDegrees;
        this.thickness = thickness;
    }

    @Override
    public boolean isHit(Location origin, Vector direction, Entity target, double reach, double rotation) {
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        Vector v = targetLoc.toVector().subtract(origin.toVector());
        double distance = v.length();

        if (distance > reach) return false;
        if (distance < 0.01) return true;

        Vector forward = direction.clone().normalize();

        Vector right = new Vector(0, 1, 0).crossProduct(forward);
        if (right.lengthSquared() < 0.001) right = new Vector(1, 0, 0);
        right.normalize();

        Vector up = forward.clone().crossProduct(right).normalize();

        double rad = Math.toRadians(rotation);
        Vector rotRight = right.clone().multiply(Math.cos(rad)).add(up.clone().multiply(Math.sin(rad)));
        Vector rotUp = right.clone().multiply(-Math.sin(rad)).add(up.clone().multiply(Math.cos(rad)));

        double localX = v.dot(rotRight);
        double localY = v.dot(rotUp);
        double localZ = v.dot(forward);

        double distXZ = Math.sqrt(localX * localX + localZ * localZ);
        if (distXZ > reach || distXZ < 0.001) return false;

        double angle = Math.toDegrees(Math.acos(localZ / distXZ));
        if (angle > angleDegrees / 2.0) return false;

        return Math.abs(localY) <= (thickness / 2.0 + target.getHeight() / 2.0);
    }

    @Override
    public double getMaxReach(double baseReach) {
        return baseReach;
    }

    @Override
    public void drawDebug(Location origin, Vector direction, double reach, double rotation) {
        double halfAngle = angleDegrees / 2.0;
        Vector forward = direction.clone().normalize();
        Vector right = new Vector(0, 1, 0).crossProduct(forward);
        if (right.lengthSquared() < 0.001) right = new Vector(1, 0, 0);
        right.normalize();
        Vector up = forward.clone().crossProduct(right).normalize();

        double radRoll = Math.toRadians(rotation);
        Vector rotRight = right.clone().multiply(Math.cos(radRoll)).add(up.clone().multiply(Math.sin(radRoll)));

        for (double d = 0.5; d <= reach; d += 0.5) {
            for (double a = -halfAngle; a <= halfAngle; a += 10.0) {
                double radA = Math.toRadians(a);
                Vector v = forward.clone().multiply(Math.cos(radA)).add(rotRight.clone().multiply(Math.sin(radA)));
                Location p = origin.clone().add(v.multiply(d));
                origin.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void spawnSlashEffect(Location origin, Vector direction, double reach, VisualType style, double rotation) {
        if (style == VisualType.HEAVY) {
            spawnHeavyEffect(origin, direction, reach);
            return;
        }

        if (style == VisualType.SWORD) {
            TurquoiseSlash.spawn(origin, direction, reach, rotation);
            spawnGenericArcParticles(origin, direction, reach, style, rotation);
            return;
        }

        spawnGenericArcParticles(origin, direction, reach, style, rotation);
    }

    private void spawnGenericArcParticles(Location origin, Vector direction, double reach, VisualType style, double rotation) {
        double halfAngle = angleDegrees / 2.0;
        Vector forward = direction.clone().normalize();
        Vector right = new Vector(0, 1, 0).crossProduct(forward);
        if (right.lengthSquared() < 0.001) right = new Vector(1, 0, 0);
        right.normalize();
        Vector up = forward.clone().crossProduct(right).normalize();

        double radRoll = Math.toRadians(rotation);
        Vector rotRight = right.clone().multiply(Math.cos(radRoll)).add(up.clone().multiply(Math.sin(radRoll)));

        for (double a = -halfAngle; a <= halfAngle; a += 10.0) {
            double radA = Math.toRadians(a);
            Vector v = forward.clone().multiply(Math.cos(radA)).add(rotRight.clone().multiply(Math.sin(radA)));
            Location p = origin.clone().add(v.multiply(reach * 0.8));

            origin.getWorld().spawnParticle(Particle.DUST, p, 1, 0.02, 0.02, 0.02, 0,
                    new Particle.DustOptions(Color.fromRGB(0, 255, 127), 1.0f));
            origin.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p, 1, 0.05, 0.05, 0.05, 0);

            if (style == VisualType.SCYTHE) {
                origin.getWorld().spawnParticle(Particle.SQUID_INK, p, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }

    private void spawnHeavyEffect(Location origin, Vector direction, double reach) {
        Vector dir = direction.clone().normalize();

        for (double i = 0.0; i <= reach * 0.5; i += 0.2) {
            Location point = origin.clone()
                    .add(dir.clone().multiply(reach * 0.5))
                    .subtract(0, i, 0);
            origin.getWorld().spawnParticle(Particle.CRIT, point, 3, 0.01, 0.01, 0.01, 0.1);
        }

        Location impactLoc = origin.clone().add(dir.clone().multiply(reach * 0.5));
        impactLoc.setY(origin.getY() - 1.5);

        origin.getWorld().spawnParticle(Particle.FLASH, impactLoc.clone().add(0, 0.1, 0), 1, 0, 0, 0, 0, Color.WHITE);
        origin.getWorld().spawnParticle(Particle.BLOCK, impactLoc, 15, 0.2, 0.1, 0.2, 0.1, org.bukkit.Material.STONE.createBlockData());
    }
}
