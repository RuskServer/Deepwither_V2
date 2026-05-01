package com.ruskserver.deepwither_V2.modules.skill.service;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillProjectile;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillProjectileExpireEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillProjectileLaunchEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillProjectileTickEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class SkillProjectileService implements Startable, Stoppable {

    private final Deepwither_V2 plugin;
    private final List<SkillProjectile> projectiles = new ArrayList<>();
    private BukkitTask task;

    @Inject
    public SkillProjectileService(Deepwither_V2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickProjectiles, 1L, 1L);
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel();
        }
        projectiles.clear();
    }

    public boolean launch(SkillProjectile projectile) {
        SkillProjectileLaunchEvent event = new SkillProjectileLaunchEvent(projectile);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            projectile.remove();
            return false;
        }
        projectiles.add(projectile);
        return true;
    }

    private void tickProjectiles() {
        Iterator<SkillProjectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            SkillProjectile projectile = iterator.next();
            Bukkit.getPluginManager().callEvent(new SkillProjectileTickEvent(projectile));
            boolean keep = projectile.tick();
            if (!keep) {
                iterator.remove();
                Bukkit.getPluginManager().callEvent(new SkillProjectileExpireEvent(projectile));
            }
        }
    }
}
