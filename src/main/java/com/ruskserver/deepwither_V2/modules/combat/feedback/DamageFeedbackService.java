package com.ruskserver.deepwither_V2.modules.combat.feedback;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * ダメージを受けたエンティティへの視覚・音響フィードバックを提供するサービス。
 *
 * <p>提供するフィードバック:
 * <ul>
 *   <li>ハートアニメーション（エンティティが赤く点滅する）— PacketEvents でクライアントに直接送信</li>
 *   <li>ダメージサウンド — プレイヤーは {@code ENTITY_PLAYER_HURT}、それ以外は {@code ENTITY_GENERIC_HURT}</li>
 * </ul>
 */
@Service
public class DamageFeedbackService {

    private static final byte HURT_STATUS = 2;

    /**
     * ダメージを受けたエンティティに対し、ハートアニメーションとダメージサウンドを再生します。
     *
     * @param target ダメージを受けたエンティティ
     */
    public void playHurtFeedback(LivingEntity target) {
        if (target == null || !target.isValid()) return;

        World world = target.getWorld();
        double maxDistSq = Math.pow(Bukkit.getViewDistance() * 16.0, 2);

        // ① ハートアニメーション（赤点滅）— 視野内の全プレイヤーにパケット送信
        WrapperPlayServerEntityStatus packet =
                new WrapperPlayServerEntityStatus(target.getEntityId(), HURT_STATUS);
        for (Player viewer : world.getPlayers()) {
            if (viewer.getLocation().distanceSquared(target.getLocation()) > maxDistSq) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }

        // ② ダメージサウンド
        Sound sound = (target instanceof Player)
                ? Sound.ENTITY_PLAYER_HURT
                : Sound.ENTITY_GENERIC_HURT;
        world.playSound(target.getLocation(), sound, 1.0f, 1.0f);
    }
}
