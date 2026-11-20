package com.ruskserver.deepwither_V2.modules.combat.melee;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.modules.combat.melee.MeleeWeaponArchetype.WeaponPose;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 三人称視点用の近接武器 {@link ItemDisplay}（プレイヤーに {@code addPassenger} で騎乗）を
 * 60fps クライアント補間でアニメーションさせるコントローラー。
 * <p>
 * 一人称と完全同一の単一斬撃平面＋刃先向き（{@code edgeTwist = ±85°}）クォータニオンで駆動し、
 * すべての斬撃・突き・叩きつけで刃先が進行方向を先頭で切り裂く。
 */
public final class MeleeDisplayAnimator {

    // プレイヤー騎乗アタッチメントは立ち身長 1.80m 付近のため -1.80f で足元基準へ戻す
    private static final float PASSENGER_Y_BASE_OFFSET = -1.80f;

    private final Deepwither_V2 plugin;
    private final Map<UUID, ItemDisplay> activeDisplays = new ConcurrentHashMap<>();

    public MeleeDisplayAnimator(Deepwither_V2 plugin) {
        this.plugin = plugin;
    }

    /**
     * 3P ItemDisplay 空間（{@code +Z}=正面, {@code +Y}=上, {@code -X}=右, {@code +X}=左）で、
     * 刃先が必ず進行方向を向く単一斬撃平面クォータニオンを生成する。
     * <ul>
     *   <li>{@code edgeTwistDeg}: +85°＝左向き刃（右→左斬り）, -85°＝右向き刃（左→右斬り）, 0°＝正面/下向き刃（縦斬り・突き）</li>
     *   <li>{@code pitchDeg}: +76°〜+86°＝刃先を正面 {@code +Z} へ倒す, -42°＝頭上へ振り上げる, +108°＝地面へ叩きつける</li>
     *   <li>{@code sweepDeg}: -65°（右）→ 0°（正面）→ +68°（左）</li>
     *   <li>{@code bankDeg}: -26°＝右高↔左低の斜め斬撃平面</li>
     * </ul>
     */
    public static Quaternionf slash3PQuat(float bankDeg, float sweepDeg, float pitchDeg, float edgeTwistDeg) {
        return new Quaternionf()
                .rotateZ((float) Math.toRadians(bankDeg))
                .rotateY((float) Math.toRadians(sweepDeg))
                .rotateX((float) Math.toRadians(pitchDeg))
                .rotateY((float) Math.toRadians(edgeTwistDeg));
    }

    /**
     * プレイヤーに騎乗する三人称武器ディスプレイへ、クォータニオン＋位置でポーズを 60fps 補間適用する。
     *
     * @param rightOffset +＝プレイヤーの右手側, -＝左手側
     * @param heightY     足元からの高さ（胸部＝1.15f, 頭上＝1.85f）
     * @param forwardZ    プレイヤー正面方向オフセット（+＝前方）
     */
    public void applyPoseQuat(Player player, ItemStack weaponItem,
                              float rightOffset, float heightY, float forwardZ,
                              Quaternionf rotation, int interpolationTicks, float weaponScale) {
        if (player == null || !player.isOnline() || player.isDead() || weaponItem == null || weaponItem.getType().isAir()) {
            remove(player != null ? player.getUniqueId() : null);
            return;
        }

        Transformation transform = new Transformation(
                new Vector3f(-rightOffset, PASSENGER_Y_BASE_OFFSET + heightY, forwardZ),
                new Quaternionf(rotation),
                new Vector3f(weaponScale, weaponScale, weaponScale),
                new Quaternionf()
        );

        ItemDisplay display = activeDisplays.get(player.getUniqueId());
        if (display == null || !display.isValid() || !display.getWorld().equals(player.getWorld())) {
            remove(player.getUniqueId());
            display = spawnPassengerDisplay(player, weaponItem, transform);
            if (display == null) return;
            activeDisplays.put(player.getUniqueId(), display);
        } else if (!player.getPassengers().contains(display)) {
            player.addPassenger(display);
        }

        display.setRotation(player.getLocation().getYaw(), 0.0f);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(Math.max(1, interpolationTicks));
        display.setTransformation(transform);
    }

    /**
     * 既存の {@link WeaponPose} をそのまま適用する互換メソッド。
     */
    public void applyPose(Player player, ItemStack weaponItem, WeaponPose pose, int interpolationTicks, float weaponScale) {
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(pose.yawDeg()))
                .rotateX((float) Math.toRadians(pose.pitchDeg()))
                .rotateZ((float) Math.toRadians(pose.rollDeg()));
        applyPoseQuat(player, weaponItem, pose.offsetX(), pose.offsetY(), pose.offsetZ(),
                rotation, interpolationTicks, weaponScale);
    }

    public void syncYaw(Player player) {
        ItemDisplay display = activeDisplays.get(player.getUniqueId());
        if (display != null && display.isValid()) {
            display.setRotation(player.getLocation().getYaw(), 0.0f);
        }
    }

    public void remove(UUID playerId) {
        if (playerId == null) return;
        ItemDisplay display = activeDisplays.remove(playerId);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public void clearAll() {
        for (ItemDisplay display : activeDisplays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        activeDisplays.clear();
    }

    private ItemDisplay spawnPassengerDisplay(Player player, ItemStack weaponItem, Transformation initialTransform) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return null;

        ItemStack displayItem = weaponItem.clone();
        MeleeFirstPersonAnimator.clearPoseOnStack(displayItem);
        ItemDisplay spawned = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.setItemStack(displayItem);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setShadowRadius(0.0f);
            d.setShadowStrength(0.0f);
            d.setViewRange(1.5f);
            d.setTeleportDuration(1);
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(1);
            d.setRotation(loc.getYaw(), 0.0f);
            d.setTransformation(initialTransform);
        });

        // 本人の一人称視界は塞がない（他プレイヤーからは 60fps 三人称モーションが見える）
        player.hideEntity(plugin, spawned);
        player.addPassenger(spawned);
        return spawned;
    }

    public void cleanupOffline() {
        activeDisplays.keySet().removeIf(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline() || p.isDead()) {
                ItemDisplay d = activeDisplays.get(uuid);
                if (d != null && d.isValid()) d.remove();
                return true;
            }
            return false;
        });
    }
}
