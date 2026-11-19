package com.ruskserver.deepwither_V2.modules.combat.feedback;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
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
 *   <li>視界の揺れ (Hurt Animation) — プレイヤーがダメージを受けた際、視界を揺らすパケットを送信</li>
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
        playHurtFeedback(target, 0f);
    }

    /**
     * ダメージを受けたエンティティに対し、フィードバックを再生します。
     *
     * @param target ダメージを受けたエンティティ
     * @param yaw    ダメージを受けた方向（視界の揺れの方向に影響）
     */
    public void playHurtFeedback(LivingEntity target, float yaw) {
        if (target == null || !target.isValid()) return;

        World world = target.getWorld();
        double maxDistSq = Math.pow(Bukkit.getViewDistance() * 16.0, 2);

        // ① ハートアニメーション（赤点滅）— 視野内の全プレイヤーにパケット送信
        WrapperPlayServerEntityStatus statusPacket =
                new WrapperPlayServerEntityStatus(target.getEntityId(), HURT_STATUS);
        
        for (Player viewer : world.getPlayers()) {
            if (viewer.getLocation().distanceSquared(target.getLocation()) > maxDistSq) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, statusPacket);
        }

        // ② 視界の揺れ (プレイヤーのみ)
        if (target instanceof Player player) {
            WrapperPlayServerHurtAnimation hurtPacket = new WrapperPlayServerHurtAnimation(player.getEntityId(), yaw);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, hurtPacket);
        }

        // ③ ダメージサウンド
        Sound sound = (target instanceof Player)
                ? Sound.ENTITY_PLAYER_HURT
                : Sound.ENTITY_GENERIC_HURT;
        world.playSound(target.getLocation(), sound, 1.0f, 1.0f);
    }

    /**
     * 回避（EVADE）成功時の軽快な風切り音とクラウド演出を再生します。
     */
    public void playEvadeFeedback(LivingEntity target) {
        if (target == null || !target.isValid()) return;
        Location loc = target.getLocation().add(0.0, target.getHeight() * 0.55, 0.0);
        World world = loc.getWorld();
        if (world == null) return;

        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.65f);
        world.spawnParticle(Particle.CLOUD, loc, 6, 0.25, 0.25, 0.25, 0.04);
    }

    /**
     * ブロック（BLOCK）成功時の盾ガード音と火花演出を再生します。
     */
    public void playBlockFeedback(LivingEntity target) {
        if (target == null || !target.isValid()) return;
        Location loc = target.getLocation().add(0.0, target.getHeight() * 0.6, 0.0);
        World world = loc.getWorld();
        if (world == null) return;

        world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 0.9f, 1.05f);
        world.spawnParticle(Particle.CRIT, loc, 8, 0.22, 0.22, 0.22, 0.08);
    }

    /**
     * マジックシールド吸収・破壊時のシアン共鳴演出を再生します。
     */
    public void playMagicShieldHitFeedback(LivingEntity target, boolean broken) {
        if (target == null || !target.isValid()) return;
        Location loc = target.getLocation().add(0.0, target.getHeight() * 0.6, 0.0);
        World world = loc.getWorld();
        if (world == null) return;

        Particle.DustOptions cyanDust = new Particle.DustOptions(Color.fromRGB(92, 230, 255), broken ? 1.35f : 1.0f);
        if (broken) {
            world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.35f);
            world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.85f);
            world.spawnParticle(Particle.DUST, loc, 16, 0.35, 0.45, 0.35, 0.02, cyanDust);
        } else {
            world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.65f, 1.4f);
            world.spawnParticle(Particle.DUST, loc, 8, 0.28, 0.35, 0.28, 0.01, cyanDust);
        }
    }

    /**
     * 深淵（ABYSS）等のマジックシールド貫通ヒット時の紫エフェクトを再生します。
     */
    public void playAbyssBypassFeedback(LivingEntity target) {
        if (target == null || !target.isValid()) return;
        Location loc = target.getLocation().add(0.0, target.getHeight() * 0.55, 0.0);
        World world = loc.getWorld();
        if (world == null) return;

        Particle.DustOptions abyssDust = new Particle.DustOptions(Color.fromRGB(168, 72, 255), 1.25f);
        world.playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 0.9f, 0.7f);
        world.spawnParticle(Particle.DUST, loc, 12, 0.3, 0.4, 0.3, 0.02, abyssDust);
    }
}
