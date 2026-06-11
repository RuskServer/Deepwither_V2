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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class BlizzardSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public BlizzardSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "blizzard"; }

    @Override
    public String getDisplayName() { return "ブリザード"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "指定地点に極寒の吹雪を発生させ、範囲内の敵を継続的に凍てつかせる。",
                "4秒間、周囲5mの敵に0.6秒ごとに魔法ダメージ(60%)とスロウIIを与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.LOCATION; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "ice", "area"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK, SkillTag.Role.CONTROL); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.DISPLACE); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.MAGICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 60.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(12); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();

        Location targetLoc = context.getTargetLocation();
        if (targetLoc == null) {
            var start = player.getEyeLocation();
            var dir = start.getDirection();
            RayTraceResult result = player.getWorld().rayTraceBlocks(start, dir, 10.0, FluidCollisionMode.NEVER, false);
            if (result != null && result.getHitBlock() != null) {
                targetLoc = result.getHitPosition().toLocation(player.getWorld());
            } else {
                targetLoc = start.clone().add(dir.multiply(10.0));
            }
        }

        final Location center = targetLoc;
        var world = center.getWorld();
        if (world == null) return CastResult.fail();

        world.playSound(center, Sound.ITEM_TRIDENT_RETURN, 1.0f, 0.0f);

        startBlizzard(player, center);

        return CastResult.success();
    }

    private void startBlizzard(LivingEntity caster, Location center) {
        new BukkitRunnable() {
            int ticks = 0;
            final int MAX_TICKS = 80;

            @Override
            public void run() {
                if (ticks >= MAX_TICKS || !caster.isValid()) {
                    cancel();
                    return;
                }

                var world = center.getWorld();

                drawBlockRing(center, 5.0, 30, Material.ICE);
                drawBlockRing(center, 3.0, 24, Material.ICE);

                for (int i = 0; i < 2; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.0;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    var cloudLoc = center.clone().add(x, 1.5, z);
                    world.spawnParticle(Particle.CLOUD, cloudLoc, 2, 0.1, 0.1, 0.1, 0.1);
                }

                Collection<Entity> targets = world.getNearbyEntities(center, 5.0, 5.0, 5.0);
                for (Entity entity : targets) {
                    if (entity instanceof LivingEntity target && !entity.equals(caster)) {
                        double distSq = entity.getLocation().distanceSquared(center);
                        if (distSq >= 1.0 && distSq <= 25.0) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2, 1));
                        }
                    }
                }

                if (ticks % 2 == 0) {
                    world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
                }

                if (ticks % 10 == 0 && ticks < 60) {
                    for (Entity entity : targets) {
                        if (entity instanceof LivingEntity target && !entity.equals(caster)) {
                            double distSq = entity.getLocation().distanceSquared(center);
                            if (distSq >= 1.0 && distSq <= 25.0) {
                                damagePipelineManager.processScaledDamage(caster, target, DamageType.MAGIC, 0.6, getTags());
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 1L);
    }

    private void drawBlockRing(Location center, double radius, int points, Material material) {
        var world = center.getWorld();
        var blockData = Bukkit.createBlockData(material);
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 / points) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            var p = center.clone().add(x, 0.1, z);
            world.spawnParticle(Particle.BLOCK, p, 3, 0.1, 0, 0.1, 0, blockData);
        }
    }
}
