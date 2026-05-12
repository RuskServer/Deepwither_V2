package com.ruskserver.deepwither_V2.modules.combat.shape;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public interface HitShape {

    boolean isHit(Location origin, Vector direction, Entity target, double reach, double rotation);

    double getMaxReach(double baseReach);

    void drawDebug(Location origin, Vector direction, double reach, double rotation);

    void spawnSlashEffect(Location origin, Vector direction, double reach, VisualType style, double rotation);

    enum VisualType {
        SWORD,
        HEAVY,
        SPEAR,
        SCYTHE,
        DEFAULT
    }
}
