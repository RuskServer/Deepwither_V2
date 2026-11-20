package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import com.ruskserver.deepwither_V2.modules.skill.util.AnimeVfxHelper;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class FlamePillarSkill implements Skill {

    private static final double CAST_DISTANCE = 4.5;
    private static final int SEGMENT_COUNT = 5;
    private static final double SEGMENT_SPACING = 1.65;
    private static final int DURATION_TICKS = 100; // 5.0秒
    private static final int PULSE_INTERVAL_TICKS = 10; // 0.5秒ごと DoT
    private static final Color WALL_COLOR = Color.fromRGB(255, 102, 24);
    private static final Vector UP = new Vector(0, 1, 0);

    private final Deepwither_V2 plugin;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public FlamePillarSkill(Deepwither_V2 plugin, DamagePipelineManager damagePipelineManager) {
        this.plugin = plugin;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "flame_pillar"; }

    @Override
    public String getDisplayName() { return "フレイムウォール"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "視線前方に幅約8mの巨大な紅蓮の炎壁（フレイムウォール）を噴出・展開する。",
                "展開時に魔法ダメージ(140%)を与え、5秒間壁に触れた敵へ0.5秒ごとに継続魔法ダメージ(45%)＋炎上を付与する。"
        );
    }

    @Override
    public Material getIcon() { return Material.BLAZE_ROD; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "fire", "area", "dot"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 45.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(8); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        World world = player.getWorld();
        Vector forward = context.getDirection().clone().setY(0);
        if (forward.lengthSquared() < 1e-6) {
            forward = player.getEyeLocation().getDirection().setY(0);
        }
        if (forward.lengthSquared() < 1e-6) {
            forward = new Vector(0, 0, 1);
        } else {
            forward.normalize();
        }

        // 視線に垂直な横軸（壁の展開方向）
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        Location wallCenter = player.getLocation().add(forward.clone().multiply(CAST_DISTANCE));
        // eoa:vfx_flame_wall はローカル X 軸に沿って伸びているため、ローカル X を right に揃える
        float wallYawRad = (float) Math.atan2(-right.getZ(), right.getX());

        List<Location> segmentLocations = new ArrayList<>(SEGMENT_COUNT);
        int half = SEGMENT_COUNT / 2;
        for (int i = -half; i <= half; i++) {
            Location raw = wallCenter.clone().add(right.clone().multiply(i * SEGMENT_SPACING));
            Location ground = snapToGround(raw, player.getLocation().getY());
            segmentLocations.add(ground);

            // 中央ほど高く堂々とした炎の壁に
            float heightScale = (i == 0) ? 2.15f : (Math.abs(i) == 1 ? 1.95f : 1.72f);
            float widthScale = 1.65f;
            AnimeVfxHelper.spawnFlameWallSegment(plugin, ground, wallYawRad, widthScale, heightScale, WALL_COLOR, DURATION_TICKS);
            if (Math.abs(i) <= 1) {
                AnimeVfxHelper.spawnShockwave(plugin, ground.clone().add(0, 0.08, 0), 0.4, 2.6, WALL_COLOR, 9, UP);
            }
        }

        // 重厚な炎壁噴出サウンド（3層）
        world.playSound(wallCenter, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.72f);
        world.playSound(wallCenter, Sound.ENTITY_BLAZE_SHOOT, 0.95f, 0.65f);
        world.playSound(wallCenter, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.65f, 1.15f);

        // 初撃バーストダメージ (140%)
        applyWallDamage(player, segmentLocations, 1.40, 200L);

        // 5秒間の継続炎上 DoT (0.5秒ごと 45%)
        new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                elapsed += PULSE_INTERVAL_TICKS;
                if (elapsed >= DURATION_TICKS || !player.isOnline()) {
                    cancel();
                    return;
                }
                if (elapsed % 20 == 0) {
                    world.playSound(wallCenter, Sound.BLOCK_FIRE_AMBIENT, 0.85f, 0.9f);
                }
                applyWallDamage(player, segmentLocations, 0.45, 350L);
            }
        }.runTaskTimer(plugin, PULSE_INTERVAL_TICKS, PULSE_INTERVAL_TICKS);

        return CastResult.success();
    }

    private void applyWallDamage(org.bukkit.entity.Player caster, List<Location> segments, double coeff, long iFrameMs) {
        Set<UUID> hitThisPulse = new HashSet<>();
        for (Location seg : segments) {
            World world = seg.getWorld();
            if (world == null) continue;
            for (Entity entity : world.getNearbyEntities(seg.clone().add(0, 1.4, 0), 1.45, 2.2, 1.45)) {
                if (!(entity instanceof LivingEntity target) || target.equals(caster) || target.isDead()) {
                    continue;
                }
                if (!hitThisPulse.add(target.getUniqueId())) {
                    continue;
                }
                damagePipelineManager.processScaledDamage(caster, target, DamageType.MAGIC, coeff, getTags(), getId(), iFrameMs);
                target.setFireTicks(Math.max(target.getFireTicks(), 50));
            }
        }
    }

    private static Location snapToGround(Location raw, double fallbackY) {
        World world = raw.getWorld();
        if (world == null) return raw;
        int baseY = (int) Math.floor(fallbackY);
        for (int dy = 3; dy >= -4; dy--) {
            Block b = world.getBlockAt(raw.getBlockX(), baseY + dy, raw.getBlockZ());
            Block above = world.getBlockAt(raw.getBlockX(), baseY + dy + 1, raw.getBlockZ());
            if (b.getType().isSolid() && !above.getType().isSolid()) {
                return new Location(world, raw.getX(), baseY + dy + 1.0, raw.getZ());
            }
        }
        return new Location(world, raw.getX(), fallbackY, raw.getZ());
    }
}
