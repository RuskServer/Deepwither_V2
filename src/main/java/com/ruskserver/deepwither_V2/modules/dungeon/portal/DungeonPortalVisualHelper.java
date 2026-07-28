package com.ruskserver.deepwither_V2.modules.dungeon.portal;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * ダンジョン入口と脱出地点に、プレイヤーの方向を向く縦長のポータルを描画します。
 */
public final class DungeonPortalVisualHelper {

    private static final double PORTAL_WIDTH = 1.15;
    private static final double PORTAL_HEIGHT = 1.55;
    private static final int OUTER_POINTS = 24;
    private static final int INNER_POINTS = 18;

    private DungeonPortalVisualHelper() {
    }

    public static void spawnPortal(
            Player viewer,
            Location base,
            Color outerColor,
            Color innerColor
    ) {
        if (viewer == null || base == null || base.getWorld() == null
                || !viewer.getWorld().equals(base.getWorld())) {
            return;
        }

        Location center = base.clone().add(0.0, PORTAL_HEIGHT, 0.0);
        Vector toViewer = viewer.getEyeLocation().toVector().subtract(center.toVector()).setY(0.0);
        if (toViewer.lengthSquared() < 0.0001) {
            toViewer = new Vector(0.0, 0.0, 1.0);
        } else {
            toViewer.normalize();
        }
        Vector right = new Vector(toViewer.getZ(), 0.0, -toViewer.getX()).normalize();

        spawnEllipse(viewer, center, right, PORTAL_WIDTH, PORTAL_HEIGHT,
                outerColor, OUTER_POINTS, 12, 0.0);
        spawnEllipse(viewer, center, right, PORTAL_WIDTH * 0.78, PORTAL_HEIGHT * 0.80,
                innerColor, INNER_POINTS, 10, 12.0);

        double time = System.currentTimeMillis() / 350.0;
        for (int i = 0; i < 4; i++) {
            double angle = time + i * Math.PI / 2.0;
            Vector offset = right.clone().multiply(Math.cos(angle) * PORTAL_WIDTH * 0.68);
            offset.setY(Math.sin(angle) * PORTAL_HEIGHT * 0.68);
            Location spawn = center.clone().add(offset);
            Vector inward = center.toVector().subtract(spawn.toVector()).normalize();
            viewer.spawnParticle(
                    Particle.ENCHANT,
                    spawn,
                    0,
                    inward.getX(),
                    inward.getY(),
                    inward.getZ(),
                    0.18
            );
        }
        viewer.spawnParticle(Particle.END_ROD, center, 1, 0.12, 0.3, 0.12, 0.01);
    }

    public static boolean isInside(Location playerLocation, Location portalBase) {
        if (playerLocation == null || portalBase == null
                || playerLocation.getWorld() == null
                || !playerLocation.getWorld().equals(portalBase.getWorld())) {
            return false;
        }
        double dx = playerLocation.getX() - portalBase.getX();
        double dz = playerLocation.getZ() - portalBase.getZ();
        double vertical = playerLocation.getY() - portalBase.getY();
        return dx * dx + dz * dz <= 1.15 * 1.15
                && vertical >= -0.5
                && vertical <= PORTAL_HEIGHT * 2.0 + 0.5;
    }

    private static void spawnEllipse(
            Player viewer,
            Location center,
            Vector right,
            double horizontalRadius,
            double verticalRadius,
            Color color,
            int points,
            int duration,
            double angleOffset
    ) {
        double step = Math.PI * 2.0 / points;
        for (int i = 0; i < points; i++) {
            double fromAngle = step * i + Math.toRadians(angleOffset);
            double toAngle = step * (i + 1) + Math.toRadians(angleOffset);
            Location from = ellipsePoint(center, right, horizontalRadius, verticalRadius, fromAngle);
            Location to = ellipsePoint(center, right, horizontalRadius, verticalRadius, toAngle);
            viewer.spawnParticle(
                    Particle.TRAIL,
                    from,
                    1,
                    0, 0, 0,
                    0,
                    new Particle.Trail(to, color, duration)
            );
        }
    }

    private static Location ellipsePoint(
            Location center,
            Vector right,
            double horizontalRadius,
            double verticalRadius,
            double angle
    ) {
        return center.clone()
                .add(right.clone().multiply(Math.cos(angle) * horizontalRadius))
                .add(0.0, Math.sin(angle) * verticalRadius, 0.0);
    }
}
