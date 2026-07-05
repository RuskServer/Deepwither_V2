package com.ruskserver.deepwither_V2.modules.skill.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * {@link Particle#TRAIL} を使って円・円弧をワールドに描画するヘルパー。
 *
 * <p>Trail パーティクルは spawnParticle の呼び出し位置から {@code Trail#target} へ
 * 向かうトレイルを表示する。本ヘルパーでは円周上の各点をスポーン位置とし、
 * 円の中心を target として渡すことで、内向きのトレイルが密集して円に見える。</p>
 *
 * <h3>基本的な使い方</h3>
 * <pre>{@code
 * // XZ 水平面に半径 3m、青い円を 36 点で描く
 * TrailCircleHelper.spawnCircle(center, 3.0, Color.BLUE, 20, 36);
 *
 * // 法線ベクトルを指定して傾いた円を描く（Y+ 軸を前方にチルト）
 * TrailCircleHelper.spawnCircle(center, 3.0, Color.AQUA, 20, 36,
 *         new Vector(0, 1, 1).normalize(), 0);
 *
 * // 0°〜180° の半円弧だけ描く
 * TrailCircleHelper.spawnArc(center, 3.0, Color.RED, 20, 36, 0, 180,
 *         new Vector(0, 1, 0), 0);
 * }</pre>
 */
public final class TrailCircleHelper {

    private TrailCircleHelper() {}

    // -----------------------------------------------------------------------
    // 完全円（シンプル版）
    // -----------------------------------------------------------------------

    /**
     * XZ 水平面に完全な円を描く。
     *
     * @param center   円の中心位置
     * @param radius   半径（メートル）
     * @param color    パーティクルの色
     * @param duration トレイルの継続時間（ticks、1以上）
     * @param points   円周上のパーティクル数（多いほど滑らか）
     */
    public static void spawnCircle(
            Location center,
            double radius,
            Color color,
            int duration,
            int points) {
        spawnCircle(center, radius, color, duration, points, new Vector(0, 1, 0), 0.0);
    }

    /**
     * 任意の法線ベクトル・開始角を指定して完全な円を描く。
     *
     * @param center          円の中心位置
     * @param radius          半径（メートル）
     * @param color           パーティクルの色
     * @param duration        トレイルの継続時間（ticks、1以上）
     * @param points          円周上のパーティクル数
     * @param normal          円面の法線ベクトル（正規化不要）
     * @param startAngleDeg   開始角度（度）。法線と直交する基準軸からの回転オフセット。
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

    // -----------------------------------------------------------------------
    // 円弧
    // -----------------------------------------------------------------------

    /**
     * XZ 水平面に円弧を描く。
     *
     * @param center        円の中心位置
     * @param radius        半径（メートル）
     * @param color         パーティクルの色
     * @param duration      トレイルの継続時間（ticks、1以上）
     * @param points        円弧上のパーティクル数
     * @param fromDeg       開始角度（度）
     * @param toDeg         終了角度（度）
     */
    public static void spawnArc(
            Location center,
            double radius,
            Color color,
            int duration,
            int points,
            double fromDeg,
            double toDeg) {
        spawnArc(center, radius, color, duration, points, fromDeg, toDeg,
                new Vector(0, 1, 0), 0.0);
    }

    /**
     * 任意の法線ベクトル・開始角を指定して円弧を描く。
     *
     * <p>法線ベクトルは円面の向きを決める。例えば {@code new Vector(0,1,0)} なら
     * XZ 水平面、{@code new Vector(1,0,0)} なら YZ 垂直面になる。</p>
     *
     * @param center        円の中心位置
     * @param radius        半径（メートル）
     * @param color         パーティクルの色
     * @param duration      トレイルの継続時間（ticks、1以上）
     * @param points        円弧上のパーティクル数（完全円換算ではなく弧上の点数）
     * @param fromDeg       開始角度（度）
     * @param toDeg         終了角度（度）
     * @param normal        円面の法線ベクトル（正規化不要）
     * @param startAngleDeg 法線と直交する基準軸からの回転オフセット（度）
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
        if (world == null) return;
        if (points <= 0 || radius <= 0) return;
        if (duration <= 0) duration = 1;

        // 法線方向に直交する2つの基底ベクトルを求める
        Vector n = normal.clone().normalize();
        Vector u = perpendicular(n).normalize();
        Vector v = n.clone().crossProduct(u).normalize();

        // 開始角オフセットを u に適用して回転
        double offsetRad = Math.toRadians(startAngleDeg);
        Vector uRot = u.clone().multiply(Math.cos(offsetRad)).add(v.clone().multiply(Math.sin(offsetRad)));
        Vector vRot = u.clone().multiply(-Math.sin(offsetRad)).add(v.clone().multiply(Math.cos(offsetRad)));

        Particle.Trail trailData = null; // target は各点で決まるので都度生成

        double arcSpan = toDeg - fromDeg;
        // 完全円のとき最後の点が最初と重複しないよう点数分割
        boolean fullCircle = Math.abs(arcSpan % 360) < 0.001;
        int divisions = fullCircle ? points : Math.max(1, points - 1);
        double stepDeg = arcSpan / divisions;

        for (int i = 0; i < points; i++) {
            double angleDeg = fromDeg + stepDeg * i;
            double rad = Math.toRadians(angleDeg);

            double cosA = Math.cos(rad);
            double sinA = Math.sin(rad);

            // 円周上の点（スポーン位置）
            Location spawnLoc = center.clone().add(
                    uRot.clone().multiply(cosA * radius).add(vRot.clone().multiply(sinA * radius))
            );

            // target = 円の中心（内向きのトレイルが円を強調）
            world.spawnParticle(
                    Particle.TRAIL,
                    spawnLoc,
                    1,          // count
                    0, 0, 0,    // offset
                    0,          // extra
                    new Particle.Trail(center.clone(), color, duration)
            );
        }
    }

    // -----------------------------------------------------------------------
    // ユーティリティ
    // -----------------------------------------------------------------------

    /**
     * 与えられたベクトルに直交する（ゼロでない）ベクトルを返す。
     */
    private static Vector perpendicular(Vector n) {
        // n と線形独立な軸を選んでクロス積で直交ベクトルを得る
        Vector axis = (Math.abs(n.getX()) < 0.9) ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        return axis.crossProduct(n);
    }
}
