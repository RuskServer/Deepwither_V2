package com.ruskserver.deepwither_V2.modules.skill.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * {@link ItemDisplay}（60fps クライアント補間＋フルブライト発光＋動的 RGB 染色）と
 * 多層パーティクルを組み合わせたアニメ調リッチ VFX エンジン。
 * <p>
 * EoA_pack の {@code eoa:vfx_*} モデルを使用し、衝撃波リング・三日月斬撃・
 * 足元回転四角シール・高速回転近接追従 AoE・魔法陣・インパクト閃光・深淵リフト・ビームを描画する。
 */
public final class AnimeVfxHelper {

    public enum VfxShape {
        SHOCKWAVE_RING("vfx_shockwave_ring"),
        SLASH_CRESCENT("vfx_slash_crescent"),
        SQUARE_SEAL("vfx_square_seal"),
        MAGIC_CIRCLE("vfx_magic_circle"),
        SPIN_VORTEX("vfx_spin_vortex"),
        IMPACT_BURST("vfx_impact_burst"),
        ABYSS_RIFT("vfx_abyss_rift"),
        BEAM_RIBBON("vfx_beam_ribbon"),
        FLAME_WALL("vfx_flame_wall"),
        FROST_PILLAR("vfx_frost_pillar");

        private final Key itemModelKey;

        VfxShape(String modelName) {
            this.itemModelKey = Key.key("eoa", modelName);
        }

        public Key itemModelKey() {
            return itemModelKey;
        }
    }

    private static final Vector UP = new Vector(0, 1, 0);

    private AnimeVfxHelper() {
    }

    /**
     * アニメ風・高速拡大衝撃波リング（60fps 補間＋白熱コア二重リング＋砂煙/火花パーティクル補助）。
     */
    public static BukkitTask spawnShockwave(
            Plugin plugin,
            Location center,
            double startRadius,
            double endRadius,
            Color color,
            int durationTicks,
            Vector normal) {
        World world = center.getWorld();
        if (world == null) return null;
        int totalTicks = Math.max(4, durationTicks);
        Location fixedCenter = zeroRotation(center);
        Vector n = (normal == null || normal.lengthSquared() < 1e-6) ? UP.clone() : normal.clone().normalize();
        Quaternionf baseOrient = orientationFromNormal(n);
        Vector[] basis = orthonormalBasis(n);
        Color safeColor = color == null ? Color.WHITE : color;
        Color coreColor = blendToWhite(safeColor, 0.55);

        float startScale = (float) Math.max(0.25, startRadius * 2.0);
        float endScale = (float) Math.max(startScale + 0.2, endRadius * 2.0);

        ItemDisplay outerRing = spawnDisplay(fixedCenter, VfxShape.SHOCKWAVE_RING, safeColor,
                new Vector3f(startScale, 1.0f, startScale), new Quaternionf(baseOrient), 1);
        ItemDisplay innerCore = spawnDisplay(fixedCenter.clone().add(n.clone().multiply(0.04)), VfxShape.SHOCKWAVE_RING, coreColor,
                new Vector3f(startScale * 0.78f, 1.0f, startScale * 0.78f), new Quaternionf(baseOrient), 1);

        // 初動インパクト閃光＋放射火花
        world.spawnParticle(Particle.FLASH, fixedCenter, 1, 0, 0, 0, 0, safeColor);
        spawnRadialSparks(world, fixedCenter, Math.max(0.4, startRadius), endRadius, safeColor, 14);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !outerRing.isValid()) {
                    safeRemove(outerRing);
                    safeRemove(innerCore);
                    cancel();
                    return;
                }

                double progress = (double) tick / totalTicks;
                double eased = easeOutCubic(progress);
                float curScale = (float) (startScale + (endScale - startScale) * eased);
                float fadeThickness = (float) Math.max(0.05, 1.0 - progress * 0.85);
                // 終了間際はスッと収束させて消す（不自然なパツ切り防止）
                float tailShrink = progress > 0.80 ? (float) ((1.0 - progress) / 0.20) : 1.0f;
                float spinRad = (float) (progress * Math.PI * 0.35);

                Quaternionf rotOuter = new Quaternionf(baseOrient).rotateY(spinRad);
                Quaternionf rotInner = new Quaternionf(baseOrient).rotateY(-spinRad * 1.4f);

                applyTransform(outerRing, new Vector3f(curScale, fadeThickness, curScale), rotOuter, 1);
                if (innerCore.isValid()) {
                    float coreScale = curScale * 0.86f * Math.max(0.15f, tailShrink);
                    applyTransform(innerCore, new Vector3f(coreScale, fadeThickness, coreScale), rotInner, 1);
                }

                // 法線平面に沿った砂煙＋残光ダスト
                if (tick <= Math.max(2, totalTicks * 2 / 3) && tick % 2 == 1) {
                    double ringR = curScale * 0.45;
                    int dustCount = Math.min(18, Math.max(6, (int) (ringR * 2.5)));
                    Particle.DustTransition dust = new Particle.DustTransition(coreColor, safeColor, 1.15f);
                    for (int i = 0; i < dustCount; i++) {
                        double a = (Math.PI * 2.0 * i / dustCount) + tick * 0.25;
                        double ca = Math.cos(a);
                        double sa = Math.sin(a);
                        double ox = (basis[0].getX() * ca + basis[1].getX() * sa) * ringR + n.getX() * 0.06;
                        double oy = (basis[0].getY() * ca + basis[1].getY() * sa) * ringR + n.getY() * 0.06;
                        double oz = (basis[0].getZ() * ca + basis[1].getZ() * sa) * ringR + n.getZ() * 0.06;
                        Location p = fixedCenter.clone().add(ox, oy, oz);
                        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0.06, 0.04, 0.06, 0, dust);
                        if (i % 3 == 0) {
                            double vx = (basis[0].getX() * ca + basis[1].getX() * sa) * 0.12 + n.getX() * 0.04;
                            double vy = (basis[0].getY() * ca + basis[1].getY() * sa) * 0.12 + n.getY() * 0.04;
                            double vz = (basis[0].getZ() * ca + basis[1].getZ() * sa) * 0.12 + n.getZ() * 0.04;
                            world.spawnParticle(Particle.CLOUD, p, 0, vx, vy, vz, 1.0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * アニメ風・三日月斬撃アーク（高速スイング＋任意ロール傾き＋残光スパーク）。
     *
     * @param rollDeg 斬撃の傾き角（0=水平薙ぎ払い, +26=右高→左低斜め斬り, +90=真上→真下縦斬り）
     */
    public static BukkitTask spawnCrescentSlash(
            Plugin plugin,
            Location center,
            Vector forwardDirection,
            double radius,
            Color color,
            double rollDeg,
            int durationTicks) {
        return spawnCrescentSlash(plugin, center, forwardDirection, radius, color, rollDeg, false, durationTicks);
    }

    /**
     * アニメ風・三日月斬撃アーク（3D 視点ピッチ追従＋スイング方向明示指定版）。
     *
     * @param rollDeg      斬撃面の傾き角（+26=右高↔左低, +90=縦斬り）
     * @param reverseSweep false=右→左 / 上→下, true=左→右
     */
    public static BukkitTask spawnCrescentSlash(
            Plugin plugin,
            Location center,
            Vector forwardDirection,
            double radius,
            Color color,
            double rollDeg,
            boolean reverseSweep,
            int durationTicks) {
        World world = center.getWorld();
        if (world == null) return null;
        int totalTicks = Math.max(3, durationTicks);
        Location fixedCenter = zeroRotation(center);
        Color safeColor = color == null ? Color.WHITE : color;
        Color coreColor = blendToWhite(safeColor, 0.65);

        Vector fullDir = (forwardDirection == null || forwardDirection.lengthSquared() < 1e-6)
                ? new Vector(0, 0, 1)
                : forwardDirection.clone().normalize();
        Vector horiz = fullDir.clone().setY(0);
        if (horiz.lengthSquared() < 1e-6) horiz = new Vector(0, 0, 1);
        else horiz.normalize();

        // ItemDisplay(yaw=0, pitch=0) は内部で 180° Y反転描画するため、モデルの +X 三日月刃先はローカル -X にある。
        // rotateY(baseYaw) * (-1, 0, 0) = (-cos(y), 0, +sin(y)) = (horiz.x, 0, horiz.z) => baseYaw = atan2(horiz.z, -horiz.x)
        // さらに rotateZ(-pitchRad) で視点の上下ピッチ角にも追従させる。
        float baseYaw = (float) Math.atan2(horiz.getZ(), -horiz.getX());
        float pitchRad = (float) Math.asin(Math.max(-0.92, Math.min(0.92, fullDir.getY())));
        float rollRad = (float) Math.toRadians(rollDeg);
        float startSweep = (float) Math.toRadians(reverseSweep ? 65.0 : -65.0);
        float endSweep = (float) Math.toRadians(reverseSweep ? -70.0 : 70.0);
        float baseScale = (float) Math.max(1.2, radius * 2.60);

        Quaternionf initRot = new Quaternionf()
                .rotateY(baseYaw)
                .rotateZ(-pitchRad)
                .rotateX(rollRad)
                .rotateY(startSweep);

        ItemDisplay blade = spawnDisplay(fixedCenter, VfxShape.SLASH_CRESCENT, safeColor,
                new Vector3f(baseScale * 0.95f, 1.0f, baseScale * 0.95f), initRot, 1);
        ItemDisplay core = spawnDisplay(fixedCenter.clone().add(0, 0.03, 0), VfxShape.SLASH_CRESCENT, coreColor,
                new Vector3f(baseScale * 0.85f, 1.0f, baseScale * 0.85f), initRot, 1);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !blade.isValid()) {
                    safeRemove(blade);
                    safeRemove(core);
                    cancel();
                    return;
                }
                double p = (double) tick / totalTicks;
                double eased = easeOutCubic(p);
                float sweep = (float) (startSweep + (endSweep - startSweep) * eased);
                float scale = (float) (baseScale * (0.95 + 0.33 * eased) * (p > 0.72 ? (1.0 - (p - 0.72) / 0.28 * 0.45) : 1.0));

                Quaternionf rot = new Quaternionf()
                        .rotateY(baseYaw)
                        .rotateZ(-pitchRad)
                        .rotateX(rollRad)
                        .rotateY(sweep);

                applyTransform(blade, new Vector3f(scale, 1.0f, scale), rot, 1);
                if (core.isValid()) {
                    applyTransform(core, new Vector3f(scale * 0.90f, 1.0f, scale * 0.90f), rot, 1);
                }

                // 刃先の軌跡（ローカル -X 方向）に火花を散らす
                Vector3f tipLocal = new Vector3f((float) (-radius * 0.95), 0, 0).rotate(rot);
                Location tipLoc = fixedCenter.clone().add(tipLocal.x, tipLocal.y, tipLocal.z);
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, tipLoc, 2, 0.12, 0.12, 0.12, 0,
                        new Particle.DustTransition(coreColor, safeColor, 1.1f));
                world.spawnParticle(Particle.CRIT, tipLoc, 1, 0.1, 0.1, 0.1, 0.08);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 足元で回転する四角魔方陣シール（バフ・詠唱・テレポート・防壁・領域展開向け）。
     * {@code anchor} を渡すと対象エンティティの足元に 60fps 追従し、{@code null} なら固定座標に展開する。
     */
    public static BukkitTask spawnRotatingSquare(
            Plugin plugin,
            Entity anchor,
            Location fixedCenter,
            double radius,
            Color color,
            int durationTicks,
            double degreesPerTick,
            boolean counterRotateInner) {
        Location startLoc = resolveAnchorFeet(anchor, fixedCenter);
        if (startLoc == null || startLoc.getWorld() == null) return null;
        int totalTicks = Math.max(5, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(255, 225, 95) : color;
        Color innerColor = blendToWhite(safeColor, 0.45);
        float targetScale = (float) Math.max(0.5, radius * 2.0);

        ItemDisplay outer = spawnDisplay(startLoc, VfxShape.SQUARE_SEAL, safeColor,
                new Vector3f(0.2f, 1.0f, 0.2f), new Quaternionf(), 2);
        ItemDisplay inner = counterRotateInner
                ? spawnDisplay(startLoc.clone().add(0, 0.03, 0), VfxShape.SQUARE_SEAL, innerColor,
                new Vector3f(0.15f, 1.0f, 0.15f), new Quaternionf().rotateY((float) (Math.PI / 4.0)), 2)
                : null;

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                Location loc = resolveAnchorFeet(anchor, fixedCenter);
                if (tick > totalTicks || loc == null || !outer.isValid()) {
                    safeRemove(outer);
                    safeRemove(inner);
                    cancel();
                    return;
                }

                // 出現フェードイン (4 ticks) / 終了フェードアウト (5 ticks)
                float envelope = 1.0f;
                if (tick <= 4) {
                    envelope = (float) easeOutCubic(tick / 4.0);
                } else if (totalTicks - tick < 5) {
                    envelope = Math.max(0.05f, (totalTicks - tick) / 5.0f);
                }

                float s = targetScale * envelope;
                float angleRad = (float) Math.toRadians(tick * degreesPerTick);

                outer.teleport(loc);
                applyTransform(outer, new Vector3f(s, 1.0f, s), new Quaternionf().rotateY(angleRad), 1);

                if (inner != null && inner.isValid()) {
                    inner.teleport(loc.clone().add(0, 0.03, 0));
                    float innerS = s * 0.72f;
                    applyTransform(inner, new Vector3f(innerS, 1.0f, innerS),
                            new Quaternionf().rotateY((float) (Math.PI / 4.0) - angleRad * 1.35f), 1);
                }

                // 四隅からさりげなく上昇パーティクル
                if (tick % 3 == 0 && loc.getWorld() != null) {
                    double r = radius * 0.78 * envelope;
                    double a = angleRad + (tick / 3) * (Math.PI / 2.0);
                    Location corner = loc.clone().add(Math.cos(a) * r, 0.08, Math.sin(a) * r);
                    loc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, corner, 1, 0.04, 0.08, 0.04, 0,
                            new Particle.DustTransition(innerColor, safeColor, 0.9f));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 高速回転・近接追従 AoE オーラ（ワールウィンド・サイクロン・回転斬撃向け）。
     * キャスターの周囲を 60fps で高速回転しながら追従し、二重ブレード＋外周スパークを纏う。
     */
    public static BukkitTask spawnSpinningMeleeAura(
            Plugin plugin,
            Entity caster,
            double radius,
            Color color,
            int durationTicks,
            double degreesPerTick) {
        if (caster == null || !caster.isValid()) return null;
        int totalTicks = Math.max(4, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(110, 240, 255) : color;
        Color bladeColor = blendToWhite(safeColor, 0.50);
        float targetScale = (float) Math.max(0.8, radius * 2.0);
        Location startLoc = caster.getLocation().add(0, 0.95, 0);

        ItemDisplay vortex = spawnDisplay(startLoc, VfxShape.SPIN_VORTEX, safeColor,
                new Vector3f(targetScale * 0.4f, 1.0f, targetScale * 0.4f), new Quaternionf(), 1);
        ItemDisplay slashRing = spawnDisplay(startLoc.clone().add(0, 0.12, 0), VfxShape.SLASH_CRESCENT, bladeColor,
                new Vector3f(targetScale * 0.45f, 1.0f, targetScale * 0.45f), new Quaternionf(), 1);
        ItemDisplay footSquare = spawnDisplay(caster.getLocation().add(0, 0.08, 0), VfxShape.SQUARE_SEAL, safeColor,
                new Vector3f(targetScale * 0.35f, 1.0f, targetScale * 0.35f), new Quaternionf(), 1);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !caster.isValid() || caster.isDead() || !vortex.isValid()) {
                    safeRemove(vortex);
                    safeRemove(slashRing);
                    safeRemove(footSquare);
                    cancel();
                    return;
                }

                float envelope = 1.0f;
                if (tick <= 3) {
                    envelope = (float) easeOutCubic(tick / 3.0);
                } else if (totalTicks - tick < 4) {
                    envelope = Math.max(0.05f, (totalTicks - tick) / 4.0f);
                }

                Location waist = zeroRotation(caster.getLocation().add(0, 0.95, 0));
                Location feet = zeroRotation(caster.getLocation().add(0, 0.08, 0));
                float angleRad = (float) Math.toRadians(tick * degreesPerTick);
                float s = targetScale * envelope;

                vortex.teleport(waist);
                applyTransform(vortex, new Vector3f(s, 1.0f, s),
                        new Quaternionf().rotateY(angleRad), 1);

                if (slashRing.isValid()) {
                    slashRing.teleport(waist.clone().add(0, 0.12, 0));
                    float tilt = (float) Math.sin(tick * 0.45) * 0.18f;
                    applyTransform(slashRing, new Vector3f(s * 1.08f, 1.0f, s * 1.08f),
                            new Quaternionf().rotateX(tilt).rotateY(angleRad * 1.35f), 1);
                }

                if (footSquare.isValid()) {
                    footSquare.teleport(feet);
                    applyTransform(footSquare, new Vector3f(s * 0.78f, 1.0f, s * 0.78f),
                            new Quaternionf().rotateY(-angleRad * 0.45f), 1);
                }

                // 高速回転する外周スパーク＆風切り
                World world = waist.getWorld();
                if (world != null) {
                    double r = radius * 0.9 * envelope;
                    for (int k = 0; k < 2; k++) {
                        double a = angleRad + k * Math.PI;
                        Location edge = waist.clone().add(Math.cos(a) * r, (k == 0 ? 0.1 : -0.1), Math.sin(a) * r);
                        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, edge, 1, 0.08, 0.08, 0.08, 0,
                                new Particle.DustTransition(bladeColor, safeColor, 1.05f));
                        if (tick % 2 == 0) {
                            world.spawnParticle(Particle.CRIT, edge, 1, 0.05, 0.05, 0.05, 0.06);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 六芒星・ルーン魔法陣（詠唱・回復陣・範囲予兆・聖域向け）。
     */
    public static BukkitTask spawnMagicCircle(
            Plugin plugin,
            Entity anchor,
            Location fixedCenter,
            double radius,
            Color color,
            int durationTicks,
            double degreesPerTick) {
        Location startLoc = resolveAnchorFeet(anchor, fixedCenter);
        if (startLoc == null || startLoc.getWorld() == null) return null;
        int totalTicks = Math.max(5, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(135, 215, 255) : color;
        float targetScale = (float) Math.max(0.6, radius * 2.0);

        ItemDisplay circle = spawnDisplay(startLoc, VfxShape.MAGIC_CIRCLE, safeColor,
                new Vector3f(0.2f, 1.0f, 0.2f), new Quaternionf(), 2);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                Location loc = resolveAnchorFeet(anchor, fixedCenter);
                if (tick > totalTicks || loc == null || !circle.isValid()) {
                    safeRemove(circle);
                    cancel();
                    return;
                }
                float env = tick <= 4 ? (float) easeOutCubic(tick / 4.0)
                        : (totalTicks - tick < 5 ? Math.max(0.05f, (totalTicks - tick) / 5.0f) : 1.0f);
                float s = targetScale * env;
                float rot = (float) Math.toRadians(tick * degreesPerTick);
                circle.teleport(loc);
                applyTransform(circle, new Vector3f(s, 1.0f, s), new Quaternionf().rotateY(rot), 1);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 3D クロス閃光インパクトバースト（着弾・クリティカル・爆発・スラム用）。
     */
    public static BukkitTask spawnImpactBurst(
            Plugin plugin,
            Location center,
            double radius,
            Color color,
            int durationTicks) {
        World world = center.getWorld();
        if (world == null) return null;
        int totalTicks = Math.max(4, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(255, 235, 130) : color;
        float maxScale = (float) Math.max(0.6, radius * 2.0);

        ItemDisplay burst = spawnDisplay(center, VfxShape.IMPACT_BURST, blendToWhite(safeColor, 0.35),
                new Vector3f(maxScale * 0.3f, maxScale * 0.3f, maxScale * 0.3f), new Quaternionf(), 1);
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0, safeColor);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !burst.isValid()) {
                    safeRemove(burst);
                    cancel();
                    return;
                }
                double p = (double) tick / totalTicks;
                float s = (float) (maxScale * (0.35 + 0.85 * easeOutCubic(p)) * (p > 0.65 ? (1.0 - (p - 0.65) / 0.35) : 1.0));
                s = Math.max(0.05f, s);
                Quaternionf rot = new Quaternionf().rotateY((float) (p * 1.4)).rotateZ((float) (p * 0.8));
                applyTransform(burst, new Vector3f(s, s, s), rot, 1);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 深淵（ABYSS）用の歪んだ次元裂け目リフト。
     */
    public static BukkitTask spawnAbyssRift(
            Plugin plugin,
            Location center,
            double radius,
            Color color,
            int durationTicks) {
        World world = center.getWorld();
        if (world == null) return null;
        int totalTicks = Math.max(6, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(178, 72, 255) : color;
        Color darkColor = Color.fromRGB(72, 20, 128);
        float maxScale = (float) Math.max(0.8, radius * 2.0);

        ItemDisplay rift = spawnDisplay(center, VfxShape.ABYSS_RIFT, safeColor,
                new Vector3f(0.2f, 1.0f, 0.2f), new Quaternionf(), 2);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !rift.isValid()) {
                    safeRemove(rift);
                    cancel();
                    return;
                }
                // 展開・消滅時のみスケール補間し、展開中は判定半径でピタッと固定して回転のみ
                float env = tick <= 5 ? (float) easeOutCubic(tick / 5.0)
                        : (totalTicks - tick < 6 ? Math.max(0.05f, (totalTicks - tick) / 6.0f) : 1.0f);
                float s = maxScale * env;
                applyTransform(rift, new Vector3f(s, 1.0f, s), new Quaternionf().rotateY((float) (-tick * 0.14)), 1);

                if (tick % 2 == 0) {
                    double a = tick * 0.9;
                    double r = radius * 0.65 * env;
                    Location p = center.clone().add(Math.cos(a) * r, 0.12, Math.sin(a) * r);
                    world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 2, 0.08, 0.15, 0.08, 0,
                            new Particle.DustTransition(safeColor, darkColor, 1.25f));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * アニメ風エネルギービーム（クロス平面発光リボン＋収束フェード）。
     */
    public static BukkitTask spawnEnergyBeam(
            Plugin plugin,
            Location start,
            Vector direction,
            double length,
            double width,
            Color color,
            int durationTicks) {
        World world = start.getWorld();
        if (world == null || direction == null || direction.lengthSquared() < 1e-6 || length <= 0.1) return null;
        int totalTicks = Math.max(4, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(110, 210, 255) : color;
        Vector dir = direction.clone().normalize();
        Location midpoint = start.clone().add(dir.clone().multiply(length * 0.5));

        // BEAM_RIBBON はローカル +X 軸に沿って伸びているため、+X を dir に回転させる
        Quaternionf rot = new Quaternionf().rotationTo(new Vector3f(1, 0, 0),
                new Vector3f((float) dir.getX(), (float) dir.getY(), (float) dir.getZ()));
        float lenF = (float) length;
        float wF = (float) Math.max(0.2, width);

        ItemDisplay beam = spawnDisplay(midpoint, VfxShape.BEAM_RIBBON, safeColor,
                new Vector3f(lenF, wF, wF), rot, 1);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !beam.isValid()) {
                    safeRemove(beam);
                    cancel();
                    return;
                }
                double p = (double) tick / totalTicks;
                float curW = (float) Math.max(0.03, wF * (1.0 - p * p));
                Quaternionf spun = new Quaternionf(rot).rotateX((float) (p * Math.PI * 0.8));
                applyTransform(beam, new Vector3f(lenF, curW, curW), spun, 1);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 斬撃ヒット時の定番「角度を変えた2方向の閃光斬線（✕クロス）」。
     * ヒットした敵の胴体中心から2本の鋭い斬線が別角度に瞬間伸長して細く消える。
     */
    public static BukkitTask spawnHitSlashCross(
            Plugin plugin,
            Location targetCenter,
            Color color,
            double length,
            int durationTicks) {
        World world = targetCenter.getWorld();
        if (world == null) return null;
        int totalTicks = Math.max(3, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(255, 240, 175) : color;
        Color coreColor = blendToWhite(safeColor, 0.65);

        java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
        float yawBase = (float) (rng.nextDouble() * Math.PI * 2.0);
        float pitch1 = (float) Math.toRadians(28.0 + rng.nextDouble() * 24.0);
        float pitch2 = (float) Math.toRadians(-(28.0 + rng.nextDouble() * 24.0));
        float yaw2 = yawBase + (float) Math.toRadians(40.0 + rng.nextDouble() * 55.0);

        Quaternionf rot1 = new Quaternionf().rotateY(yawBase).rotateZ(pitch1);
        Quaternionf rot2 = new Quaternionf().rotateY(yaw2).rotateZ(pitch2);

        float maxLen = (float) Math.max(0.8, length);
        float startLen = maxLen * 0.35f;
        float startThick = 0.34f;

        ItemDisplay line1 = spawnDisplay(targetCenter, VfxShape.BEAM_RIBBON, coreColor,
                new Vector3f(startLen, startThick, startThick), rot1, 1);
        ItemDisplay line2 = spawnDisplay(targetCenter, VfxShape.BEAM_RIBBON, safeColor,
                new Vector3f(startLen * 0.9f, startThick * 0.9f, startThick * 0.9f), rot2, 1);

        world.spawnParticle(Particle.CRIT, targetCenter, 5, 0.15, 0.15, 0.15, 0.12);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !line1.isValid()) {
                    safeRemove(line1);
                    safeRemove(line2);
                    cancel();
                    return;
                }
                double p = (double) tick / totalTicks;
                double eased = easeOutCubic(p);
                float curLen = (float) (startLen + (maxLen - startLen) * eased);
                float curThick = (float) Math.max(0.02, startThick * (1.0 - p * p));

                applyTransform(line1, new Vector3f(curLen, curThick, curThick), rot1, 1);
                if (line2.isValid()) {
                    applyTransform(line2, new Vector3f(curLen * 0.92f, curThick * 0.9f, curThick * 0.9f), rot2, 1);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * プロジェクタイル発射時のマズル衝撃波（進行方向に垂直な衝撃波リング＋瞬間展開スクエアシール）。
     */
    public static BukkitTask spawnProjectileMuzzle(
            Plugin plugin,
            Location muzzleLoc,
            Vector direction,
            double radius,
            Color color,
            int durationTicks) {
        World world = muzzleLoc.getWorld();
        if (world == null || direction == null || direction.lengthSquared() < 1e-6) return null;
        int totalTicks = Math.max(4, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(170, 225, 255) : color;
        Color coreColor = blendToWhite(safeColor, 0.55);
        Quaternionf orient = orientationFromNormal(direction);

        float startScale = (float) Math.max(0.25, radius * 0.6);
        float endScale = (float) Math.max(startScale + 0.3, radius * 2.2);

        ItemDisplay ring = spawnDisplay(muzzleLoc, VfxShape.SHOCKWAVE_RING, safeColor,
                new Vector3f(startScale, 1.0f, startScale), new Quaternionf(orient), 1);
        ItemDisplay seal = spawnDisplay(muzzleLoc.clone().add(direction.clone().normalize().multiply(0.05)), VfxShape.SQUARE_SEAL, coreColor,
                new Vector3f(startScale * 0.7f, 1.0f, startScale * 0.7f), new Quaternionf(orient), 1);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !ring.isValid()) {
                    safeRemove(ring);
                    safeRemove(seal);
                    cancel();
                    return;
                }
                double p = (double) tick / totalTicks;
                double eased = easeOutCubic(p);
                float curS = (float) (startScale + (endScale - startScale) * eased);
                float tail = p > 0.7 ? (float) Math.max(0.05, (1.0 - p) / 0.3) : 1.0f;

                applyTransform(ring, new Vector3f(curS, tail, curS),
                        new Quaternionf(orient).rotateY((float) (p * 0.8)), 1);
                if (seal.isValid()) {
                    float sealS = (float) (radius * 1.45 * tail);
                    applyTransform(seal, new Vector3f(sealS, 1.0f, sealS),
                            new Quaternionf(orient).rotateY((float) (-p * 1.6)), 1);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * プロジェクタイル飛翔中のソニックブーム衝撃波リング（進行方向に垂直に展開する軽量1エンティティ）。
     * 飛翔中 2〜4 tick ごとに呼ぶことで、弾道に鋭い空気の輪が連なるアニメ表現になる。
     */
    public static BukkitTask spawnProjectileSonicRing(
            Plugin plugin,
            Location location,
            Vector direction,
            double startRadius,
            double endRadius,
            Color color,
            int durationTicks) {
        World world = location.getWorld();
        if (world == null || direction == null || direction.lengthSquared() < 1e-6) return null;
        int totalTicks = Math.max(3, durationTicks);
        Color safeColor = color == null ? Color.fromRGB(185, 235, 255) : color;
        Quaternionf orient = orientationFromNormal(direction);

        float startScale = (float) Math.max(0.2, startRadius * 2.0);
        float endScale = (float) Math.max(startScale + 0.15, endRadius * 2.0);

        ItemDisplay ring = spawnDisplay(location, VfxShape.SHOCKWAVE_RING, safeColor,
                new Vector3f(startScale, 1.0f, startScale), new Quaternionf(orient), 1);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !ring.isValid()) {
                    safeRemove(ring);
                    cancel();
                    return;
                }
                double p = (double) tick / totalTicks;
                float curS = (float) (startScale + (endScale - startScale) * easeOutCubic(p));
                float fade = (float) Math.max(0.05, 1.0 - p * 0.9);
                applyTransform(ring, new Vector3f(curS, fade, curS),
                        new Quaternionf(orient).rotateY((float) (p * 0.6)), 1);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * プロジェクタイル着弾時の衝撃波＋3D インパクト閃光。
     */
    public static void spawnProjectileImpact(
            Plugin plugin,
            Location impactLoc,
            Vector impactNormal,
            double radius,
            Color color,
            int durationTicks) {
        if (impactLoc == null || impactLoc.getWorld() == null) return;
        Vector n = (impactNormal == null || impactNormal.lengthSquared() < 1e-6) ? UP : impactNormal;
        spawnShockwave(plugin, impactLoc, Math.max(0.25, radius * 0.25), radius, color, durationTicks, n);
        spawnImpactBurst(plugin, impactLoc, Math.max(0.8, radius * 0.65), color, Math.max(4, durationTicks - 2));
    }

    /**
     * 専用 3D ボクセルモデル {@code eoa:vfx_flame_wall}（.mcmeta アニメーション炎テクスチャ）を使った「フレイムウォール」1セグメント。
     * 噴出後は本体を上下に揺らさずどっしり固定し、テクスチャアニメーションと上昇する火の粉・煙パーティクルで自然な炎を表現する。
     */
    public static BukkitTask spawnFlameWallSegment(
            Plugin plugin,
            Location groundLoc,
            float wallYawRad,
            float widthScale,
            float heightScale,
            Color color,
            int durationTicks) {
        World world = groundLoc.getWorld();
        if (world == null) return null;
        int totalTicks = Math.max(12, durationTicks);
        Location baseLoc = zeroRotation(groundLoc);

        Quaternionf rot = new Quaternionf().rotateY(wallYawRad);
        float wF = Math.max(0.6f, widthScale);
        float hF = Math.max(0.8f, heightScale);

        ItemDisplay wall = spawnDisplay(baseLoc, VfxShape.FLAME_WALL, null,
                new Vector3f(wF, 0.08f, wF * 0.72f), new Quaternionf(rot), 2);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !wall.isValid()) {
                    safeRemove(wall);
                    cancel();
                    return;
                }

                // 最初の4tickで出現・最後の6tickで収束。展開中は上下に伸縮させずどっしり固定！
                if (tick <= 4) {
                    float sy = Math.max(0.08f, (float) (hF * easeOutCubic(tick / 4.0)));
                    applyTransformWithOffset(wall, new Vector3f(0.0f, 0.5f * sy, 0.0f),
                            new Vector3f(wF, sy, wF * 0.72f), rot, 1);
                } else if (totalTicks - tick < 6) {
                    float sy = Math.max(0.05f, hF * ((totalTicks - tick) / 6.0f));
                    applyTransformWithOffset(wall, new Vector3f(0.0f, 0.5f * sy, 0.0f),
                            new Vector3f(wF, sy, wF * 0.72f), rot, 1);
                }

                // 浮遊する火の粉・上昇炎パーティクルのみ演出
                if (tick % 3 == 0 && tick <= totalTicks - 4) {
                    world.spawnParticle(Particle.FLAME, baseLoc.clone().add(0, 0.5 + hF * 0.55, 0),
                            3, 0.55, 0.65, 0.55, 0.025);
                    if (tick % 6 == 0) {
                        world.spawnParticle(Particle.LAVA, baseLoc.clone().add(0, 0.35, 0),
                                1, 0.40, 0.25, 0.40, 0.0);
                        world.spawnParticle(Particle.SMOKE, baseLoc.clone().add(0, hF * 1.15, 0),
                                1, 0.35, 0.20, 0.35, 0.02);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 専用 3D ボクセル氷晶モデル {@code eoa:vfx_frost_pillar} を使った「フロストピラー」1柱。
     * 3 tick で地面から突き上げた後は固い氷柱として完全に静止し、周囲に冷気粉・氷晶パーティクルを漂わせたあと粉砕する。
     */
    public static BukkitTask spawnFrostPillarSpire(
            Plugin plugin,
            Location groundLoc,
            float radiusScale,
            float heightScale,
            Color color,
            int durationTicks) {
        World world = groundLoc.getWorld();
        if (world == null) return null;
        int totalTicks = Math.max(14, durationTicks);
        Location baseLoc = zeroRotation(groundLoc);
        Color safeColor = color == null ? Color.fromRGB(115, 225, 255) : color;
        Color coreColor = blendToWhite(safeColor, 0.60);

        float rF = Math.max(0.6f, radiusScale);
        float hF = Math.max(0.8f, heightScale);
        float randomYaw = (float) (java.util.concurrent.ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0);
        Quaternionf rot = new Quaternionf().rotateY(randomYaw);

        // 足元の氷結魔法陣＋突き上げ衝撃波
        spawnMagicCircle(plugin, null, baseLoc.clone().add(0, 0.05, 0), rF * 1.15, safeColor, Math.min(totalTicks, 20), 6.0);
        spawnShockwave(plugin, baseLoc.clone().add(0, 0.08, 0), 0.30, rF * 1.65, safeColor, 8, UP);

        ItemDisplay spire = spawnDisplay(baseLoc, VfxShape.FROST_PILLAR, null,
                new Vector3f(rF, 0.06f, rF), new Quaternionf(rot), 1);

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > totalTicks || !spire.isValid()) {
                    // 終了時に氷結晶が砕け散る演出
                    spawnImpactBurst(plugin, baseLoc.clone().add(0, hF * 0.85, 0), rF * 1.25, coreColor, 5);
                    world.spawnParticle(Particle.SNOWFLAKE, baseLoc.clone().add(0, hF * 0.75, 0),
                            16, 0.55, 0.65, 0.55, 0.08);
                    world.playSound(baseLoc, org.bukkit.Sound.BLOCK_GLASS_BREAK, 0.45f, 1.55f);
                    safeRemove(spire);
                    cancel();
                    return;
                }

                // 最初の3tickで素早く突き上げ、その後は固い氷柱としてピタッと静止（上下揺れなし）
                if (tick <= 3) {
                    float sy = Math.max(0.06f, (float) (hF * easeOutCubic(tick / 3.0)));
                    applyTransformWithOffset(spire, new Vector3f(0.0f, 0.5f * sy, 0.0f),
                            new Vector3f(rF, sy, rF), rot, 1);
                } else if (totalTicks - tick < 4) {
                    float sy = Math.max(0.10f, hF * ((totalTicks - tick) / 4.0f));
                    applyTransformWithOffset(spire, new Vector3f(0.0f, 0.5f * sy, 0.0f),
                            new Vector3f(rF, sy, rF), rot, 1);
                }

                // 漂う冷気粉・氷晶きらめき
                if (tick % 3 == 0 && tick <= totalTicks - 3) {
                    world.spawnParticle(Particle.SNOWFLAKE, baseLoc.clone().add(0, hF * 0.85, 0),
                            3, 0.45, 0.65, 0.45, 0.02);
                    if (tick % 6 == 0) {
                        world.spawnParticle(Particle.END_ROD, baseLoc.clone().add(0, hF * 1.15, 0),
                                1, 0.35, 0.45, 0.35, 0.01);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // --- 内部ヘルパー ---

    private static ItemDisplay spawnDisplay(
            Location location,
            VfxShape shape,
            Color color,
            Vector3f initialScale,
            Quaternionf initialRotation,
            int interpolationTicks) {
        ItemStack stack = createVfxItem(shape, color);
        // エンティティ自身の yaw/pitch が乗ると Transformation.leftRotation と二重回転・上下傾きバグになるため必ず 0 に固定
        Location cleanLoc = zeroRotation(location);
        return cleanLoc.getWorld().spawn(cleanLoc, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setRotation(0.0f, 0.0f);
            display.setItemStack(stack);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setShadowRadius(0.0f);
            display.setShadowStrength(0.0f);
            display.setViewRange(2.0f);
            display.setTeleportDuration(1);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(Math.max(1, interpolationTicks));
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    initialRotation,
                    initialScale,
                    new Quaternionf()
            ));
        });
    }

    private static ItemStack createVfxItem(VfxShape shape, Color color) {
        ItemStack stack = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        stack.setData(DataComponentTypes.ITEM_MODEL, shape.itemModelKey());
        if (color != null) {
            stack.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color));
        }
        return stack;
    }

    private static void applyTransform(ItemDisplay display, Vector3f scale, Quaternionf rotation, int interpTicks) {
        applyTransformWithOffset(display, new Vector3f(0, 0, 0), scale, rotation, interpTicks);
    }

    private static void applyTransformWithOffset(ItemDisplay display, Vector3f translation,
                                                 Vector3f scale, Quaternionf rotation, int interpTicks) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(Math.max(1, interpTicks));
        display.setTransformation(new Transformation(
                translation,
                rotation,
                scale,
                new Quaternionf()
        ));
    }

    private static Quaternionf orientationFromNormal(Vector normal) {
        Vector n = normal.clone().normalize();
        Vector3f target = new Vector3f((float) n.getX(), (float) n.getY(), (float) n.getZ());
        if (target.distanceSquared(0, -1, 0) < 1e-4f) {
            return new Quaternionf(new AxisAngle4f((float) Math.PI, 1, 0, 0));
        }
        return new Quaternionf().rotationTo(new Vector3f(0, 1, 0), target);
    }

    private static Location zeroRotation(Location loc) {
        return new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), 0.0f, 0.0f);
    }

    private static Vector[] orthonormalBasis(Vector normal) {
        Vector n = normal.clone().normalize();
        Vector ref = Math.abs(n.getY()) > 0.9 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector t1 = n.clone().crossProduct(ref).normalize();
        Vector t2 = n.clone().crossProduct(t1).normalize();
        return new Vector[]{t1, t2};
    }

    private static Location resolveAnchorFeet(Entity anchor, Location fixedCenter) {
        if (anchor != null) {
            if (!anchor.isValid() || anchor.isDead()) return null;
            return zeroRotation(anchor.getLocation().add(0, 0.06, 0));
        }
        return fixedCenter == null ? null : zeroRotation(fixedCenter);
    }

    private static void spawnRadialSparks(World world, Location center, double startR, double endR, Color color, int count) {
        double speed = Math.min(0.45, Math.max(0.12, (endR - startR) * 0.06));
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2.0 * i) / count;
            double dx = Math.cos(a);
            double dz = Math.sin(a);
            Location p = center.clone().add(dx * startR, 0.15, dz * startR);
            world.spawnParticle(Particle.CRIT, p, 0, dx * speed, 0.08, dz * speed, 1.0);
        }
    }

    private static Color blendToWhite(Color color, double ratio) {
        double r = Math.max(0.0, Math.min(1.0, ratio));
        int red = (int) Math.round(color.getRed() + (255 - color.getRed()) * r);
        int green = (int) Math.round(color.getGreen() + (255 - color.getGreen()) * r);
        int blue = (int) Math.round(color.getBlue() + (255 - color.getBlue()) * r);
        return Color.fromRGB(red, green, blue);
    }

    private static double easeOutCubic(double t) {
        double r = 1.0 - Math.max(0.0, Math.min(1.0, t));
        return 1.0 - r * r * r;
    }

    private static void safeRemove(Entity entity) {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }
}
