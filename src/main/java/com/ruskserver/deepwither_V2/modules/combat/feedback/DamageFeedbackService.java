package com.ruskserver.deepwither_V2.modules.combat.feedback;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@Service
public class DamageFeedbackService {

    private static final byte HURT_STATUS = 2;

    public void playHurtFeedback(LivingEntity target) {
        if (target == null || !target.isValid()) return;

        WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(target.getEntityId(), HURT_STATUS);
        World world = target.getWorld();
        double maxDistanceSquared = Math.pow(Bukkit.getViewDistance() * 16.0, 2);

        for (Player viewer : world.getPlayers()) {
            if (viewer.getLocation().distanceSquared(target.getLocation()) > maxDistanceSquared) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }
}
