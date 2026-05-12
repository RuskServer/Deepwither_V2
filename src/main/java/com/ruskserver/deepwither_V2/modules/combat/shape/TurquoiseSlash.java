package com.ruskserver.deepwither_V2.modules.combat.shape;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class TurquoiseSlash {

    public static void spawn(Location origin, Vector direction, double reach, double rotation) {
        World world = origin.getWorld();
        if (world == null) return;

        origin.getWorld().spawnParticle(Particle.DUST, origin, 20, 1.0, 1.0, 1.0, 0.0,
                new Particle.DustOptions(Color.fromRGB(0, 255, 127), 1.5f));

        Vector dirH = direction.clone().setY(0).normalize();
        Vector right = new Vector(-dirH.getZ(), 0, dirH.getX()).normalize();

        double f = 3.0;
        double y = 0.0;
        double so = 0.0;

        double baseOffset = -1.7;

        if (Math.abs(rotation - 0.0) < 1.0) {
            y = 0.1;
        } else if (Math.abs(rotation - 180.0) < 1.0) {
            y = -0.2;
        } else if (Math.abs(rotation - 45.0) < 1.0) {
            y = 0.1;
            so = -0.3;
        } else if (Math.abs(rotation - 135.0) < 1.0) {
            y = -0.2;
            so = -0.3;
        }

        Location summonLoc = origin.clone().add(dirH.clone().multiply(f)).add(0, baseOffset + y, 0).add(right.clone().multiply(so));
        summonLoc.setDirection(dirH);

        ArmorStand stand = world.spawn(summonLoc, ArmorStand.class, s -> {
            s.setMarker(true);
            s.setSmall(false);
            s.setInvisible(true);
            s.setInvulnerable(true);
            s.setBasePlate(false);
            s.setGravity(false);
            s.setCanTick(true);
            s.setHeadPose(new EulerAngle(0, 0, Math.toRadians(-rotation)));

            ItemStack modelItem = new ItemStack(Material.PAPER);
            ItemMeta meta = modelItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(7600);
                meta.setHideTooltip(true);
                modelItem.setItemMeta(meta);
            }
            s.getEquipment().setItem(EquipmentSlot.HEAD, modelItem);
        });

        new BukkitRunnable() {
            int frame = 2;

            @Override
            public void run() {
                if (frame > 7) {
                    stand.remove();
                    this.cancel();
                    return;
                }

                ItemStack modelItem = new ItemStack(Material.PAPER);
                ItemMeta meta = modelItem.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(7600 + (frame - 1));
                    meta.setHideTooltip(true);
                    modelItem.setItemMeta(meta);
                }
                stand.getEquipment().setItem(EquipmentSlot.HEAD, modelItem);

                frame++;
            }
        }.runTaskTimer(JavaPlugin.getProvidingPlugin(TurquoiseSlash.class), 0L, 1L);
    }
}
