package com.ruskserver.deepwither_V2.modules.mob.service;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class MobMagicProjectileService implements Listener, Startable, Stoppable {

    private final Deepwither_V2 plugin;
    private final DamagePipelineManager damageManager;
    private final NamespacedKey sourceKey;
    private final NamespacedKey damageKey;
    private final NamespacedKey tagsKey;
    private final Set<UUID> trackedProjectiles = new HashSet<>();
    private BukkitTask trailTask;

    @Inject
    public MobMagicProjectileService(Deepwither_V2 plugin, DamagePipelineManager damageManager) {
        this.plugin = plugin;
        this.damageManager = damageManager;
        this.sourceKey = new NamespacedKey(plugin, "mob_magic_source");
        this.damageKey = new NamespacedKey(plugin, "mob_magic_damage");
        this.tagsKey = new NamespacedKey(plugin, "mob_magic_tags");
    }

    @Override
    public void start() {
        trailTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickTrails, 1L, 1L);
    }

    @Override
    public void stop() {
        if (trailTask != null) {
            trailTask.cancel();
            trailTask = null;
        }
        for (UUID projectileId : trackedProjectiles) {
            Entity entity = Bukkit.getEntity(projectileId);
            if (entity != null) entity.remove();
        }
        trackedProjectiles.clear();
    }

    public void launch(LivingEntity caster, LivingEntity target, double speed, double damage,
                       String sourceId, Set<String> tags) {
        Vector direction = target.getEyeLocation().toVector()
                .subtract(caster.getEyeLocation().toVector());
        if (direction.lengthSquared() < 0.01) return;

        Snowball projectile = caster.launchProjectile(
                Snowball.class, direction.normalize().multiply(speed));
        projectile.setGravity(false);
        projectile.setShooter(caster);
        projectile.getPersistentDataContainer().set(sourceKey, PersistentDataType.STRING, sourceId);
        projectile.getPersistentDataContainer().set(damageKey, PersistentDataType.DOUBLE, damage);
        projectile.getPersistentDataContainer().set(
                tagsKey, PersistentDataType.STRING, tags == null ? "" : String.join(",", tags));
        trackedProjectiles.add(projectile.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Snowball projectile)) return;

        String sourceId = projectile.getPersistentDataContainer()
                .get(sourceKey, PersistentDataType.STRING);
        Double damage = projectile.getPersistentDataContainer()
                .get(damageKey, PersistentDataType.DOUBLE);
        if (sourceId == null || damage == null) return;

        event.setDamage(0.0);
        event.setCancelled(true);
        trackedProjectiles.remove(projectile.getUniqueId());

        if (!(event.getEntity() instanceof Player target)
                || !(projectile.getShooter() instanceof LivingEntity caster)) {
            return;
        }

        String serializedTags = projectile.getPersistentDataContainer()
                .getOrDefault(tagsKey, PersistentDataType.STRING, "");
        Set<String> tags = serializedTags.isBlank()
                ? Set.of()
                : Set.of(serializedTags.split(","));
        damageManager.processDamage(
                caster, target, DamageType.MAGIC, damage, tags, sourceId, 500L,
                projectile.getLocation());
        playImpact(projectile);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball projectile)) return;
        if (!projectile.getPersistentDataContainer().has(sourceKey, PersistentDataType.STRING)) return;

        trackedProjectiles.remove(projectile.getUniqueId());
        if (event.getHitBlock() != null) {
            playImpact(projectile);
        }
    }

    private void tickTrails() {
        trackedProjectiles.removeIf(projectileId -> {
            Entity entity = Bukkit.getEntity(projectileId);
            if (!(entity instanceof Snowball projectile) || !projectile.isValid()) {
                return true;
            }
            projectile.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK, projectile.getLocation(), 2,
                    0.05, 0.05, 0.05, 0.01);
            projectile.getWorld().spawnParticle(
                    Particle.DUST, projectile.getLocation(), 1,
                    0.0, 0.0, 0.0, 0.0,
                    new Particle.DustOptions(Color.fromRGB(0xA978FF), 1.0f));
            return false;
        });
    }

    private void playImpact(Snowball projectile) {
        projectile.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK, projectile.getLocation(), 14,
                0.25, 0.25, 0.25, 0.08);
        projectile.getWorld().playSound(
                projectile.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.35f, 1.8f);
    }
}
