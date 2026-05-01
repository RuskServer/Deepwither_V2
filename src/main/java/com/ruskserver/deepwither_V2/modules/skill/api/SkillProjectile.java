package com.ruskserver.deepwither_V2.modules.skill.api;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public abstract class SkillProjectile {

    private final LivingEntity caster;
    private Location currentLocation;
    private final Vector direction;
    private final double speed;
    private final double hitboxRadius;
    private final int maxTicks;
    private int ticksLived;
    private boolean removed;

    protected SkillProjectile(LivingEntity caster, Location spawnLocation, Vector direction, double speed, double hitboxRadius, int maxTicks) {
        this.caster = caster;
        this.currentLocation = spawnLocation.clone();
        this.direction = direction.clone().normalize();
        this.speed = speed;
        this.hitboxRadius = hitboxRadius;
        this.maxTicks = maxTicks;
    }

    public final boolean tick() {
        if (removed || ticksLived >= maxTicks || !caster.isValid() || currentLocation.getWorld() == null) {
            expire();
            return false;
        }

        RayTraceResult result = currentLocation.getWorld().rayTrace(
                currentLocation,
                direction,
                speed,
                FluidCollisionMode.NEVER,
                true,
                hitboxRadius,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(caster.getUniqueId())
        );

        if (result != null) {
            currentLocation = result.getHitPosition().toLocation(currentLocation.getWorld());
            if (result.getHitEntity() instanceof LivingEntity target) {
                onHitEntity(target);
                return !removed;
            }
            Block hitBlock = result.getHitBlock();
            if (hitBlock != null) {
                onHitBlock(hitBlock);
                return !removed;
            }
        }

        currentLocation.add(direction.clone().multiply(speed));
        ticksLived++;
        onTick();
        return !removed;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public int getTicksLived() {
        return ticksLived;
    }

    public final void remove() {
        removed = true;
    }

    public final boolean isRemoved() {
        return removed;
    }

    protected void expire() {
        remove();
    }

    protected abstract void onTick();

    protected abstract void onHitEntity(LivingEntity target);

    protected abstract void onHitBlock(Block block);
}
