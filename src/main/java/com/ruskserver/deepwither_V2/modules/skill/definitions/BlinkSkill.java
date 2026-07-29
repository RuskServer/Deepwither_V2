package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class BlinkSkill implements Skill {

    private static final double MAX_DISTANCE = 14.0;
    private static final double STEP = 0.5;
    private static final double MIN_USEFUL_DISTANCE_SQUARED = 1.0;
    private static final int[] VERTICAL_SEARCH_OFFSETS = {0, 1, -1, 2, -2, 3, -3, 4, -4};

    @Override
    public String getId() {
        return "blink";
    }

    @Override
    public String getDisplayName() {
        return "瞬間移動";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "0.4秒の詠唱後、視線方向の安全な地点へ瞬間移動する。",
                "最大14m先まで移動し、安全な移動先がない場合は発動しない。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.ENDER_PEARL;
    }

    @Override
    public SkillCategory getCategory() {
        return SkillCategory.ACTIVE;
    }

    @Override
    public SkillTargetType getTargetType() {
        return SkillTargetType.SELF;
    }

    @Override
    public Set<String> getTags() {
        return Set.of("mobility", "teleport");
    }

    @Override
    public Set<SkillTag.Role> getRoles() {
        return Set.of(SkillTag.Role.UTILITY);
    }

    @Override
    public Set<SkillTag.Tactic> getTactics() {
        return Set.of(SkillTag.Tactic.MOBILITY);
    }

    @Override
    public Set<SkillTag.Constraint> getConstraints() {
        return Set.of(SkillTag.Constraint.CHANNELING, SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD);
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 45.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(18);
    }

    @Override
    public Duration getCastTime(SkillContext context) {
        return Duration.ofMillis(400);
    }

    @Override
    public CastResult cast(SkillContext context) {
        Player player = context.getCaster();
        Location target = findSafeTarget(player, context.getDirection());
        if (target == null) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("安全な瞬間移動先が見つかりません。", NamedTextColor.RED));
        }

        Location from = player.getLocation().clone();
        if (!player.teleport(target)) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("瞬間移動に失敗しました。", NamedTextColor.RED));
        }

        player.getWorld().spawnParticle(Particle.PORTAL, from.add(0.0, 1.0, 0.0), 28, 0.35, 0.65, 0.35, 0.08);
        player.getWorld().spawnParticle(Particle.PORTAL, target.clone().add(0.0, 1.0, 0.0), 28, 0.35, 0.65, 0.35, 0.08);
        player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.2f);
        return CastResult.success();
    }

    private Location findSafeTarget(Player player, Vector direction) {
        Location origin = player.getLocation();
        World world = origin.getWorld();
        Vector horizontalDirection = direction.clone().setY(0.0);
        if (horizontalDirection.lengthSquared() < 1.0E-6) {
            horizontalDirection = origin.getDirection().setY(0.0);
        }
        if (horizontalDirection.lengthSquared() < 1.0E-6) {
            return null;
        }
        horizontalDirection.normalize();

        for (double distance = MAX_DISTANCE; distance >= STEP; distance -= STEP) {
            Location candidate = origin.clone().add(horizontalDirection.clone().multiply(distance));
            Location safe = findSafeFeetLocation(candidate, origin);
            if (safe != null
                    && safe.distanceSquared(origin) >= MIN_USEFUL_DISTANCE_SQUARED
                    && hasClearBlinkPath(origin, safe, world)) {
                return safe;
            }
        }
        return null;
    }

    private boolean hasClearBlinkPath(Location origin, Location target, World world) {
        Location eyePath = origin.clone().add(0.0, 1.62, 0.0);
        Location targetEye = target.clone().add(0.0, 1.62, 0.0);
        Vector path = targetEye.toVector().subtract(eyePath.toVector());
        double pathLength = path.length();
        if (pathLength < 1.0E-6) {
            return false;
        }

        RayTraceResult collision = world.rayTraceBlocks(
                eyePath,
                path.normalize(),
                pathLength,
                FluidCollisionMode.NEVER,
                true
        );
        return collision == null;
    }

    private Location findSafeFeetLocation(Location candidate, Location origin) {
        World world = candidate.getWorld();
        int blockX = candidate.getBlockX();
        int blockZ = candidate.getBlockZ();
        int baseY = origin.getBlockY();

        for (int offset : VERTICAL_SEARCH_OFFSETS) {
            int feetY = baseY + offset;
            if (feetY <= world.getMinHeight() || feetY >= world.getMaxHeight() - 1) {
                continue;
            }

            Block feet = world.getBlockAt(blockX, feetY, blockZ);
            Block head = feet.getRelative(0, 1, 0);
            Block floor = feet.getRelative(0, -1, 0);
            if (feet.isPassable() && head.isPassable() && floor.getType().isSolid()) {
                return new Location(
                        world,
                        blockX + 0.5,
                        feetY,
                        blockZ + 0.5,
                        origin.getYaw(),
                        origin.getPitch()
                );
            }
        }
        return null;
    }
}
