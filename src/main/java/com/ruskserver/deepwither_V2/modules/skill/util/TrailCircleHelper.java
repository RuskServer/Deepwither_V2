package com.ruskserver.deepwither_V2.modules.skill.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * {@link Particle#TRAIL} を使って、閉じた円周・円弧・拡大型の衝撃波を描画するヘルパー。
 *
 * <p>円周上の隣接点を TRAIL の spawn 位置と target にすることで、
 * 放射状の線ではなく連続したリングとして見えるようにする。</p>
 */
public final class TrailCircleHelper {

    private static final Vector UP = new Vector(0, 1, 0);
    private static final double MAX_SHOCKWAVE_SEGMENT_LENGTH = 0.5;
    private static final int MAX_AUTOMATIC_SHOCKWAVE_POINTS = 96;
    private static final double SHOCKWAVE_TRAILING_GAP = 0.22;
    private static final double SHOCKWAVE_TRAILING_HEIGHT = 0.08;
    private static final int SHOCKWAVE_TRAIL_DURATION = 2;

    private TrailCircleHelper() {}

    /**
     * XZ 水平面に閉じた円周を描く。
     *
     * @param center   円の中心位置
     * @param radius   半径（メートル）
     * @param color    パーティクルの色
     * @param duration 各 TRAIL が隣の円周点へ到達するまでの時間（ticks）
     * @param points   円周を構成する分割数
     */
    public static void spawnCircle(
            Location center,
            double radius,
            Color color,
            int duration,
            int points) {
        spawnCircle(center, radius, color, duration, points, UP, 0.0);
    }

    /**
     * 任意の法線ベクトル・開始角を指定して閉じた円周を描く。
     *
     * @param center        円の中心位置
     * @param radius        半径（メートル）
     * @param color         パーティクルの色
     * @param duration      各 TRAIL が隣の円周点へ到達するまでの時間（ticks）
     * @param points        円周を構成する分割数
     * @param normal        円面の法線ベクトル（正規化不要）
     * @param startAngleDeg 開始角度（度）
     */
    public static void spawnCircle(
            Location center,
            double radius,
            Color color,
            int duration,
            int points,
            Vector normal,
            double startAngleDeg) {
        spawnArc(center, radius, color, duration, points, 0.0, 360.0, normal, startAngleDeg);
    }

    /**
     * XZ 水平面に円弧を描く。
     *
     * @param center   円の中心位置
     * @param radius   半径（メートル）
     * @param color    パーティクルの色
     * @param duration 各 TRAIL が次の円周点へ到達するまでの時間（ticks）
     * @param points   円弧上の点数
     * @param fromDeg  開始角度（度）
     * @param toDeg    終了角度（度）
     */
    public static void spawnArc(
            Location center,
            double radius,
            Color color,
            int duration,
            int points,
            double fromDeg,
            double toDeg) {
        spawnArc(center, radius, color, duration, points, fromDeg, toDeg, UP, 0.0);
    }

    /**
     * 任意の法線ベクトル・開始角を指定して円弧を描く。
     *
     * <p>完全円では最後の点から最初の点へ接続して閉じる。
     * 部分円弧では両端を含む {@code points} 個の点を、隣接順に接続する。</p>
     *
     * @param center        円の中心位置
     * @param radius        半径（メートル）
     * @param color         パーティクルの色
     * @param duration      各 TRAIL が次の円周点へ到達するまでの時間（ticks）
     * @param points        円周または円弧上の点数
     * @param fromDeg       開始角度（度）
     * @param toDeg         終了角度（度）
     * @param normal        円面の法線ベクトル（正規化不要）
     * @param startAngleDeg 開始角度の回転オフセット（度）
     */
    public static void spawnArc(
            Location center,
            double radius,
            Color color,
            int duration,
            int points,
            double fromDeg,
            double toDeg,
            Vector normal,
            double startAngleDeg) {

        World world = center.getWorld();
        if (world == null || radius <= 0 || normal == null || normal.lengthSquared() == 0) return;

        double arcSpan = toDeg - fromDeg;
        boolean fullCircle = isFullCircle(arcSpan);
        if ((fullCircle && points < 3) || (!fullCircle && points < 2)) return;

        Basis basis = createBasis(normal, startAngleDeg);
        int segmentCount = fullCircle ? points : points - 1;
        double stepDeg = arcSpan / segmentCount;
        int trailDuration = Math.max(1, duration);

        for (int i = 0; i < segmentCount; i++) {
            Location from = pointOnCircle(center, radius, fromDeg + stepDeg * i, basis);
            Location to = pointOnCircle(center, radius, fromDeg + stepDeg * (i + 1), basis);
            spawnTrail(world, from, to, color, trailDuration);
        }
    }

    /**
     * 水平なリングを、開始半径から終了半径まで1tickごとに広げる。
     *
     * @param plugin         タスクを所有するプラグイン
     * @param center         衝撃波の中心位置
     * @param startRadius    開始半径（メートル）
     * @param endRadius      終了半径（メートル）
     * @param color          パーティクルの色
     * @param expansionTicks 拡大にかける時間（ticks）
     * @param minimumPoints  円周の最低分割数。大きな円では密度維持のため自動的に増える
     * @return 実行中の描画タスク
     */
    public static BukkitTask spawnExpandingShockwave(
            Plugin plugin,
            Location center,
            double startRadius,
            double endRadius,
            Color color,
            int expansionTicks,
            int minimumPoints) {
        return spawnExpandingShockwave(
                plugin, center, startRadius, endRadius, color, expansionTicks, minimumPoints, UP, 0.0);
    }

    /**
     * 任意の向きのリングを、開始半径から終了半径まで1tickごとに広げる。
     */
    public static BukkitTask spawnExpandingShockwave(
            Plugin plugin,
            Location center,
            double startRadius,
            double endRadius,
            Color color,
            int expansionTicks,
            int minimumPoints,
            Vector normal,
            double startAngleDeg) {

        if (plugin == null) throw new IllegalArgumentException("plugin must not be null");
        if (center == null || center.getWorld() == null) {
            throw new IllegalArgumentException("center must have a world");
        }
        if (startRadius <= 0 || endRadius <= 0) {
            throw new IllegalArgumentException("radii must be greater than zero");
        }
        if (expansionTicks <= 0) {
            throw new IllegalArgumentException("expansionTicks must be greater than zero");
        }
        if (minimumPoints < 3) {
            throw new IllegalArgumentException("minimumPoints must be at least 3");
        }
        if (normal == null || normal.lengthSquared() == 0) {
            throw new IllegalArgumentException("normal must not be zero");
        }

        Location fixedCenter = center.clone();
        Vector fixedNormal = normal.clone();
        Basis basis = createBasis(fixedNormal, startAngleDeg);
        Vector trailingCenterOffset = fixedNormal.clone().normalize().multiply(SHOCKWAVE_TRAILING_HEIGHT);
        double largestRadius = Math.max(startRadius, endRadius);
        int leadingPoints = Math.max(
                minimumPoints,
                Math.min(
                        MAX_AUTOMATIC_SHOCKWAVE_POINTS,
                        (int) Math.ceil(2.0 * Math.PI * largestRadius / MAX_SHOCKWAVE_SEGMENT_LENGTH)
                )
        );
        int trailingPoints = Math.max(3, leadingPoints / 3);
        double movementDirection = Math.signum(endRadius - startRadius);

        return new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                double currentProgress = (double) tick / expansionTicks;
                double nextProgress = Math.min(1.0, (double) (tick + 1) / expansionTicks);
                double currentRadius = interpolateRadius(
                        startRadius, endRadius, easeOutCubic(currentProgress)
                );
                double nextRadius = interpolateRadius(
                        startRadius, endRadius, easeOutCubic(nextProgress)
                );

                spawnRadialTransitionRing(
                        fixedCenter,
                        currentRadius,
                        nextRadius,
                        color,
                        SHOCKWAVE_TRAIL_DURATION,
                        leadingPoints,
                        basis,
                        0.0
                );

                if (movementDirection != 0.0) {
                    double trailingOffset = -movementDirection * SHOCKWAVE_TRAILING_GAP;
                    double trailingCurrentRadius = Math.max(0.05, currentRadius + trailingOffset);
                    double trailingNextRadius = Math.max(0.05, nextRadius + trailingOffset);
                    double angleOffset = 180.0 / trailingPoints;
                    spawnRadialTransitionRing(
                            fixedCenter.clone().add(trailingCenterOffset),
                            trailingCurrentRadius,
                            trailingNextRadius,
                            color,
                            SHOCKWAVE_TRAIL_DURATION + 1,
                            trailingPoints,
                            basis,
                            angleOffset
                    );
                }

                if (tick >= expansionTicks) {
                    cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void spawnRadialTransitionRing(
            Location center,
            double fromRadius,
            double toRadius,
            Color color,
            int duration,
            int points,
            Basis basis,
            double angleOffsetDeg) {
        World world = center.getWorld();
        if (world == null || fromRadius <= 0 || toRadius <= 0 || points < 3) {
            return;
        }

        double stepDeg = 360.0 / points;
        for (int i = 0; i < points; i++) {
            double angleDeg = angleOffsetDeg + stepDeg * i;
            Location from = pointOnCircle(center, fromRadius, angleDeg, basis);
            Location to = pointOnCircle(center, toRadius, angleDeg, basis);
            spawnTrail(world, from, to, color, duration);
        }
    }

    private static double interpolateRadius(double startRadius, double endRadius, double progress) {
        return startRadius + (endRadius - startRadius) * progress;
    }

    /**
     * 円周上の各点から外側へ短い TRAIL を放つ。リング形状へ動きを足す用途。
     */
    public static void spawnRadialBurstRing(
            Location center,
            double radius,
            double outwardDistance,
            Color color,
            int duration,
            int points,
            Vector normal,
            double startAngleDeg) {

        World world = center.getWorld();
        if (world == null || radius <= 0 || outwardDistance <= 0 || points < 3
                || normal == null || normal.lengthSquared() == 0) {
            return;
        }

        Basis basis = createBasis(normal, startAngleDeg);
        int trailDuration = Math.max(1, duration);
        double stepDeg = 360.0 / points;

        for (int i = 0; i < points; i++) {
            double angleDeg = stepDeg * i;
            Location from = pointOnCircle(center, radius, angleDeg, basis);
            Location to = pointOnCircle(center, radius + outwardDistance, angleDeg, basis);
            spawnTrail(world, from, to, color, trailDuration);
        }
    }

    private static void spawnTrail(
            World world,
            Location from,
            Location to,
            Color color,
            int duration) {
        world.spawnParticle(
                Particle.TRAIL,
                from,
                1,
                0, 0, 0,
                0,
                new Particle.Trail(to, color, duration)
        );
    }

    private static Location pointOnCircle(
            Location center,
            double radius,
            double angleDeg,
            Basis basis) {
        double angleRad = Math.toRadians(angleDeg);
        Vector offset = basis.u().clone().multiply(Math.cos(angleRad) * radius)
                .add(basis.v().clone().multiply(Math.sin(angleRad) * radius));
        return center.clone().add(offset);
    }

    private static Basis createBasis(Vector normal, double startAngleDeg) {
        Vector n = normal.clone().normalize();
        Vector u = perpendicular(n).normalize();
        Vector v = n.clone().crossProduct(u).normalize();

        double offsetRad = Math.toRadians(startAngleDeg);
        Vector rotatedU = u.clone().multiply(Math.cos(offsetRad))
                .add(v.clone().multiply(Math.sin(offsetRad)));
        Vector rotatedV = u.clone().multiply(-Math.sin(offsetRad))
                .add(v.clone().multiply(Math.cos(offsetRad)));
        return new Basis(rotatedU, rotatedV);
    }

    private static boolean isFullCircle(double arcSpan) {
        return Math.abs(arcSpan) >= 360.0
                && Math.abs(arcSpan % 360.0) < 0.001;
    }

    private static double easeOutCubic(double progress) {
        double remaining = 1.0 - progress;
        return 1.0 - remaining * remaining * remaining;
    }

    private static Vector perpendicular(Vector normal) {
        Vector axis = Math.abs(normal.getX()) < 0.9
                ? new Vector(1, 0, 0)
                : new Vector(0, 1, 0);
        return axis.crossProduct(normal);
    }

    private record Basis(Vector u, Vector v) {}
}
