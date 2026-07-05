package com.ruskserver.deepwither_V2.modules.skill.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public final class TrailHelper {

    private TrailHelper() {}

    public static void spawnLine(Location from, Location to, Color color, int duration) {
        World world = from.getWorld();
        if (world == null) return;
        if (duration <= 0) duration = 1;
        world.spawnParticle(Particle.TRAIL, from, 1, 0, 0, 0, 0, new Particle.Trail(to, color, duration));
    }

    public static void spawnSegmentedLine(Location from, Location to, Color color, int duration, int segments) {
        World world = from.getWorld();
        if (world == null || segments <= 0) return;
        if (duration <= 0) duration = 1;
        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / segments;
            double t1 = (double) (i + 1) / segments;
            Location p0 = from.clone().add(to.clone().subtract(from).multiply(t0));
            Location p1 = from.clone().add(to.clone().subtract(from).multiply(t1));
            world.spawnParticle(Particle.TRAIL, p0, 1, 0, 0, 0, 0, new Particle.Trail(p1, color, duration));
        }
    }

    public static void spawnBeam(Location start, Vector direction, double length, Color color, int duration, double radius) {
        World world = start.getWorld();
        if (world == null) return;
        if (duration <= 0) duration = 1;
        Vector dir = direction.clone().normalize();
        Location end = start.clone().add(dir.clone().multiply(length));

        if (radius <= 0) {
            spawnLine(start, end, color, duration);
            return;
        }

        Vector perp = perpendicular(dir);
        Vector perp2 = dir.clone().crossProduct(perp).normalize();

        int rings = Math.max(1, (int) (length / 0.8));
        for (int i = 0; i <= rings; i++) {
            double t = (double) i / rings;
            Location center = start.clone().add(dir.clone().multiply(t * length));
            for (int j = 0; j < 4; j++) {
                double angle = j * Math.PI / 2;
                Vector offset = perp.clone().multiply(Math.cos(angle) * radius)
                        .add(perp2.clone().multiply(Math.sin(angle) * radius));
                Location ringLoc = center.clone().add(offset);
                Location targetLoc = ringLoc.clone().add(dir.clone().multiply(0.5));
                world.spawnParticle(Particle.TRAIL, ringLoc, 1, 0, 0, 0, 0, new Particle.Trail(targetLoc, color, duration));
            }
        }
    }

    public static void spawnWave(Location from, Location to, Color color, int duration, int segments, double amplitude) {
        World world = from.getWorld();
        if (world == null || segments <= 0) return;
        if (duration <= 0) duration = 1;
        Vector dir = to.clone().subtract(from).toVector().normalize();
        Vector perp = perpendicular(dir);
        Vector up = dir.clone().crossProduct(perp).normalize();

        double totalLength = from.distance(to);
        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / segments;
            double t1 = (double) (i + 1) / segments;
            double offset0 = Math.sin(t0 * Math.PI * segments * 0.5) * amplitude;
            double offset1 = Math.sin(t1 * Math.PI * segments * 0.5) * amplitude;
            Location p0 = from.clone().add(dir.clone().multiply(t0 * totalLength)).add(up.clone().multiply(offset0));
            Location p1 = from.clone().add(dir.clone().multiply(t1 * totalLength)).add(up.clone().multiply(offset1));
            world.spawnParticle(Particle.TRAIL, p0, 1, 0, 0, 0, 0, new Particle.Trail(p1, color, duration));
        }
    }

    public static void spawnSpiral(Location center, Vector axis, double radius, double height, Color color, int duration, int points) {
        World world = center.getWorld();
        if (world == null || points <= 0) return;
        if (duration <= 0) duration = 1;
        Vector dir = axis.clone().normalize();
        Vector perp = perpendicular(dir);
        Vector perp2 = dir.clone().crossProduct(perp).normalize();

        for (int i = 0; i < points; i++) {
            double t = (double) i / points;
            double angle = t * Math.PI * 4;
            Vector offset = perp.clone().multiply(Math.cos(angle) * radius)
                    .add(perp2.clone().multiply(Math.sin(angle) * radius));
            Location spawnLoc = center.clone().add(dir.clone().multiply(t * height)).add(offset);
            Location targetLoc = spawnLoc.clone().add(dir.clone().multiply(0.3));
            world.spawnParticle(Particle.TRAIL, spawnLoc, 1, 0, 0, 0, 0, new Particle.Trail(targetLoc, color, duration));
        }
    }

    public static void spawnCone(Location origin, Vector direction, double length, double angleDeg, Color color, int duration, int lines, int segmentsPerLine) {
        World world = origin.getWorld();
        if (world == null || lines <= 0) return;
        if (duration <= 0) duration = 1;
        Vector dir = direction.clone().normalize();
        Vector perp = perpendicular(dir);
        Vector perp2 = dir.clone().crossProduct(perp).normalize();
        double angleRad = Math.toRadians(angleDeg);

        for (int l = 0; l < lines; l++) {
            double theta = (double) l / lines * Math.PI * 2;
            Vector coneDir = dir.clone().multiply(Math.cos(angleRad))
                    .add(perp.clone().multiply(Math.sin(angleRad) * Math.cos(theta)))
                    .add(perp2.clone().multiply(Math.sin(angleRad) * Math.sin(theta)))
                    .normalize();

            for (int i = 0; i < segmentsPerLine; i++) {
                double t0 = (double) i / segmentsPerLine;
                double t1 = (double) (i + 1) / segmentsPerLine;
                Location p0 = origin.clone().add(coneDir.clone().multiply(t0 * length));
                Location p1 = origin.clone().add(coneDir.clone().multiply(t1 * length));
                world.spawnParticle(Particle.TRAIL, p0, 1, 0, 0, 0, 0, new Particle.Trail(p1, color, duration));
            }
        }
    }

    private static Vector perpendicular(Vector n) {
        Vector axis = (Math.abs(n.getX()) < 0.9) ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        return axis.crossProduct(n).normalize();
    }
}
