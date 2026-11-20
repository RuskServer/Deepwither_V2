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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class IceSpikeSkill implements Skill {

    private static final int PILLAR_COUNT = 6;
    private static final double START_DISTANCE = 2.0;
    private static final double STEP_DISTANCE = 1.75;
    private static final int CASCADE_INTERVAL_TICKS = 2;
    private static final int SPIRE_DURATION_TICKS = 36; // 1.8秒残存→粉砕
    private static final Color FROST_COLOR = Color.fromRGB(110, 224, 255);

    private final Deepwither_V2 plugin;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public IceSpikeSkill(Deepwither_V2 plugin, DamagePipelineManager damagePipelineManager) {
        this.plugin = plugin;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "ice_spike"; }

    @Override
    public String getDisplayName() { return "フロストピラー"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "前方の大地を凍てつかせ、巨大な氷晶柱（フロストピラー）を連鎖的に突き上げる。",
                "視線方向へ最大11mまで6本の氷柱が順次噴出し、魔法ダメージ(190%)・軽い打ち上げ・移動速度低下II(3秒)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.PACKED_ICE; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "ice", "area", "cc"); }

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
        Location casterLoc = player.getLocation().clone();
        Vector dir = context.getDirection().clone().setY(0);
        if (dir.lengthSquared() < 1e-6) {
            dir = player.getEyeLocation().getDirection().setY(0);
        }
        if (dir.lengthSquared() < 1e-6) {
            dir = new Vector(0, 0, 1);
        } else {
            dir.normalize();
        }

        // 初動の氷結詠唱音
        world.playSound(casterLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9f, 1.35f);
        world.playSound(casterLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.75f, 1.15f);

        Set<UUID> alreadyHit = new HashSet<>();
        Vector finalDir = dir;
        double baseY = casterLoc.getY();

        // 2 tick ごとに前方へ連鎖噴出する 6 本の巨大 3D 氷柱！
        new BukkitRunnable() {
            private int idx = 0;

            @Override
            public void run() {
                if (idx >= PILLAR_COUNT || !player.isOnline()) {
                    cancel();
                    return;
                }
                double dist = START_DISTANCE + idx * STEP_DISTANCE;
                Location raw = casterLoc.clone().add(finalDir.clone().multiply(dist));
                Location ground = snapToGround(raw, baseY);

                // 奥へ行くほど迫力が増す巨大氷柱スケール (1.05x -> 1.55x)
                float growth = idx / (float) Math.max(1, PILLAR_COUNT - 1);
                float radiusScale = 1.15f + 0.35f * growth;
                float heightScale = 1.25f + 0.55f * growth;

                AnimeVfxHelper.spawnFrostPillarSpire(plugin, ground, radiusScale, heightScale, FROST_COLOR, SPIRE_DURATION_TICKS);

                // 氷柱噴出の多層クリスタル破砕音
                float pitch = 1.15f + idx * 0.08f;
                world.playSound(ground, Sound.BLOCK_GLASS_BREAK, 0.85f, pitch);
                world.playSound(ground, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.70f, Math.min(2.0f, pitch + 0.25f));

                for (Entity entity : world.getNearbyEntities(ground.clone().add(0, 1.5, 0), 2.35, 3.2, 2.35)) {
                    if (!(entity instanceof LivingEntity target) || target.equals(player) || target.isDead()) {
                        continue;
                    }
                    if (!alreadyHit.add(target.getUniqueId())) {
                        continue;
                    }
                    damagePipelineManager.processScaledDamage(player, target, DamageType.MAGIC, 1.90, getTags(), getId(), 400L);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
                    // 氷柱の突き上げ感
                    Vector vel = target.getVelocity();
                    target.setVelocity(new Vector(vel.getX() * 0.4, Math.max(vel.getY(), 0.28), vel.getZ() * 0.4));
                }

                idx++;
            }
        }.runTaskTimer(plugin, 0L, CASCADE_INTERVAL_TICKS);

        return CastResult.success();
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
