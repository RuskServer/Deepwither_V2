package com.ruskserver.deepwither_V2.modules.combat.feedback;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CriticalHitFeedbackService {

    private static final long FEEDBACK_INTERVAL_MILLIS = 120L;
    private static final int MAX_RECENT_HITS = 512;
    private static final int SPARK_COUNT = 10;

    private final DamageImpactResolver impactResolver;
    private final Map<HitKey, Long> recentHits = new LinkedHashMap<>();

    @Inject
    public CriticalHitFeedbackService(DamageImpactResolver impactResolver) {
        this.impactResolver = impactResolver;
    }

    public void show(DamageContext context) {
        if (context == null || !context.isCritical() || context.getDamage() <= 0.0) {
            return;
        }

        LivingEntity attacker = context.getAttacker();
        LivingEntity defender = context.getDefender();
        if (attacker == null || defender == null || !attacker.getWorld().equals(defender.getWorld())) {
            return;
        }

        long now = System.currentTimeMillis();
        HitKey key = new HitKey(attacker.getUniqueId(), defender.getUniqueId());
        Long lastFeedback = recentHits.get(key);
        if (lastFeedback != null && now - lastFeedback < FEEDBACK_INTERVAL_MILLIS) {
            return;
        }
        recentHits.put(key, now);
        trimRecentHits();

        Location impact = impactResolver.resolve(context);
        World world = impact.getWorld();
        if (world == null) {
            return;
        }

        Vector normal = attacker.getEyeLocation().toVector().subtract(impact.toVector());
        if (normal.lengthSquared() < 1.0E-6) {
            normal.setY(1.0);
        } else {
            normal.normalize();
        }

        Color color = resolveColor(context);
        world.spawnParticle(Particle.FLASH, impact, 1, 0, 0, 0, 0, Color.WHITE);
        world.spawnParticle(Particle.DUST, impact, 5, 0.08, 0.08, 0.08, 0,
                new Particle.DustOptions(color, 1.35f));
        TrailCircleHelper.spawnRadialBurstRing(
                impact, 0.16, 0.82, color, 5, 10, normal, 18.0);
        spawnVectorSparks(world, impact, normal);

        world.playSound(impact, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.08f);
        world.playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 1.75f);
    }

    private void spawnVectorSparks(World world, Location impact, Vector normal) {
        Vector axis = Math.abs(normal.getY()) < 0.9
                ? new Vector(0, 1, 0)
                : new Vector(1, 0, 0);
        Vector tangent = normal.clone().crossProduct(axis).normalize();
        Vector bitangent = normal.clone().crossProduct(tangent).normalize();

        for (int i = 0; i < SPARK_COUNT; i++) {
            double angle = Math.PI * 2.0 * i / SPARK_COUNT;
            Vector direction = tangent.clone().multiply(Math.cos(angle))
                    .add(bitangent.clone().multiply(Math.sin(angle)))
                    .add(normal.clone().multiply(0.18))
                    .normalize();
            world.spawnParticle(
                    Particle.CRIT,
                    impact,
                    0,
                    direction.getX(), direction.getY(), direction.getZ(),
                    0.2
            );
        }
    }

    private Color resolveColor(DamageContext context) {
        if (context.hasTag("FIRE")) {
            return Color.fromRGB(255, 104, 32);
        }
        if (context.hasTag("ICE")) {
            return Color.fromRGB(105, 225, 255);
        }
        if (context.hasTag("LIGHTNING") || context.hasTag("THUNDER")) {
            return Color.fromRGB(255, 235, 92);
        }
        if (context.getType() == DamageType.MAGIC) {
            return Color.fromRGB(190, 110, 255);
        }
        if (context.getType() == DamageType.TRUE_DAMAGE) {
            return Color.fromRGB(110, 255, 225);
        }
        return Color.fromRGB(255, 196, 64);
    }

    private void trimRecentHits() {
        if (recentHits.size() <= MAX_RECENT_HITS) {
            return;
        }
        Iterator<HitKey> iterator = recentHits.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private record HitKey(UUID attackerId, UUID defenderId) {
    }
}
