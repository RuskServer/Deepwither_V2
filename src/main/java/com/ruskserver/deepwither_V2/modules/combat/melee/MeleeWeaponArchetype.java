package com.ruskserver.deepwither_V2.modules.combat.melee;

import java.util.List;

/**
 * 全近接武器種（剣/マチェット・大剣・斧・槍/ハルバード・ダガー・メイス/ハンマー・鎌）の
 * コンボタイムライン・判定弧・3D ItemDisplay キーフレーム定義（純粋 Java）。
 */
public enum MeleeWeaponArchetype {

    SWORD(List.of(
            // 1段目: 右斜め薙ぎ
            new ComboStep("Slash R→L", 5, 5, 6, 1.00, 3.3, 140.0, 1.0, 0.22, false, false, -26.0,
                    new WeaponPose(0.55f, 1.25f, 0.15f, -75f, -18f, -25f),
                    new WeaponPose(0.38f, 1.22f, 0.48f, -55f, -14f, -25f),
                    new WeaponPose(-0.48f, 1.08f, 0.45f, 92f, 12f, -25f),
                    new WeaponPose(-0.35f, 1.12f, 0.30f, 35f, 0f, 0f)),
            // 2段目: 左逆斜め返し
            new ComboStep("Reverse L→R", 5, 5, 6, 1.05, 3.3, 140.0, 1.0, 0.24, false, false, 28.0,
                    new WeaponPose(-0.48f, 1.12f, 0.20f, 85f, 15f, 25f),
                    new WeaponPose(-0.32f, 1.14f, 0.48f, 55f, 12f, 25f),
                    new WeaponPose(0.52f, 1.24f, 0.45f, -88f, -12f, 25f),
                    new WeaponPose(0.35f, 1.16f, 0.30f, -30f, 0f, 0f)),
            // 3段目: 踏み込み突き (1.3x)
            new ComboStep("Step Thrust", 5, 5, 8, 1.30, 4.0, 56.0, 1.25, 0.42, true, false, 0.0,
                    new WeaponPose(0.32f, 1.12f, -0.25f, 0f, 0f, 0f),
                    new WeaponPose(0.24f, 1.14f, 0.30f, 0f, 0f, 0f),
                    new WeaponPose(0.18f, 1.14f, 0.92f, 0f, 0f, 0f),
                    new WeaponPose(0.32f, 1.12f, 0.32f, -10f, 0f, 0f))
    )),

    GREATSWORD(List.of(
            // 1段目: 重い大横薙ぎ
            new ComboStep("Wide Cleave", 8, 6, 9, 1.25, 4.2, 165.0, 1.35, 0.26, false, false, -8.0,
                    new WeaponPose(0.68f, 1.22f, -0.10f, -115f, 0f, 88f),
                    new WeaponPose(0.45f, 1.20f, 0.50f, -80f, 0f, 88f),
                    new WeaponPose(-0.62f, 1.16f, 0.48f, 115f, 0f, 88f),
                    new WeaponPose(-0.40f, 1.14f, 0.30f, 50f, 0f, 35f)),
            // 2段目: 頭上叩き割り（地面衝撃波）
            new ComboStep("Overhead Slam", 9, 6, 10, 1.60, 4.0, 95.0, 1.85, 0.32, false, true, 90.0,
                    new WeaponPose(0.16f, 1.85f, -0.18f, 0f, -105f, 0f),
                    new WeaponPose(0.14f, 1.55f, 0.38f, 0f, -35f, 0f),
                    new WeaponPose(0.14f, 0.85f, 0.62f, 0f, 45f, 0f),
                    new WeaponPose(0.28f, 1.12f, 0.32f, -10f, 12f, 0f))
    )),

    AXE(List.of(
            // 1段目: 重い叩き斬り（衝撃波）
            new ComboStep("Heavy Chop", 6, 5, 8, 1.20, 3.5, 110.0, 1.35, 0.28, false, true, -34.0,
                    new WeaponPose(0.46f, 1.72f, -0.12f, -25f, -95f, -25f),
                    new WeaponPose(0.32f, 1.45f, 0.38f, -10f, -25f, -25f),
                    new WeaponPose(-0.24f, 0.88f, 0.52f, 30f, 42f, -25f),
                    new WeaponPose(-0.18f, 1.08f, 0.30f, 15f, 10f, 0f)),
            // 2段目: 逆袈裟大薙ぎ
            new ComboStep("Reverse Cleave", 6, 5, 8, 1.38, 3.7, 150.0, 1.45, 0.30, false, false, 30.0,
                    new WeaponPose(-0.52f, 1.05f, 0.18f, 95f, 18f, 28f),
                    new WeaponPose(-0.34f, 1.12f, 0.48f, 60f, 10f, 28f),
                    new WeaponPose(0.56f, 1.26f, 0.46f, -105f, -14f, 28f),
                    new WeaponPose(0.38f, 1.16f, 0.30f, -35f, 0f, 0f))
    )),

    SPEAR(List.of(
            // 1段目: 長リーチ突き
            new ComboStep("Long Thrust", 4, 5, 6, 0.98, 4.6, 48.0, 0.95, 0.25, true, false, 0.0,
                    new WeaponPose(0.32f, 1.14f, -0.35f, 0f, 0f, 0f),
                    new WeaponPose(0.24f, 1.15f, 0.35f, 0f, 0f, 0f),
                    new WeaponPose(0.20f, 1.15f, 1.05f, 0f, 0f, 0f),
                    new WeaponPose(0.30f, 1.14f, 0.35f, 0f, 0f, 0f)),
            // 2段目: 連続突き
            new ComboStep("Quick Thrust", 4, 5, 6, 1.05, 4.6, 48.0, 1.00, 0.28, true, false, 0.0,
                    new WeaponPose(0.32f, 1.18f, -0.32f, 0f, -4f, 0f),
                    new WeaponPose(0.22f, 1.18f, 0.40f, 0f, -2f, 0f),
                    new WeaponPose(0.18f, 1.18f, 1.10f, 0f, 0f, 0f),
                    new WeaponPose(0.30f, 1.15f, 0.35f, 0f, 0f, 0f)),
            // 3段目: 広角薙ぎ払い
            new ComboStep("Halberd Sweep", 6, 6, 8, 1.28, 4.4, 165.0, 1.30, 0.22, false, false, -6.0,
                    new WeaponPose(0.58f, 1.18f, 0.10f, -105f, 0f, 88f),
                    new WeaponPose(0.36f, 1.18f, 0.48f, -65f, 0f, 88f),
                    new WeaponPose(-0.58f, 1.18f, 0.46f, 110f, 0f, 88f),
                    new WeaponPose(-0.34f, 1.15f, 0.30f, 35f, 0f, 20f))
    )),

    DAGGER(List.of(
            // 超高速 3 連（Active 4 ticks 確保で視認性＋疾走感を両立）
            new ComboStep("Quick Cut", 3, 4, 4, 0.68, 2.7, 125.0, 0.65, 0.18, false, false, -28.0,
                    new WeaponPose(0.45f, 1.20f, 0.20f, -72f, -15f, -28f),
                    new WeaponPose(0.28f, 1.18f, 0.48f, -45f, -8f, -28f),
                    new WeaponPose(-0.38f, 1.10f, 0.45f, 78f, 10f, -28f),
                    new WeaponPose(-0.20f, 1.12f, 0.32f, 25f, 0f, 0f)),
            new ComboStep("Back Cut", 3, 4, 4, 0.72, 2.7, 125.0, 0.65, 0.20, false, false, 28.0,
                    new WeaponPose(-0.40f, 1.12f, 0.22f, 78f, 12f, 28f),
                    new WeaponPose(-0.22f, 1.14f, 0.48f, 45f, 8f, 28f),
                    new WeaponPose(0.42f, 1.22f, 0.45f, -78f, -10f, 28f),
                    new WeaponPose(0.26f, 1.16f, 0.32f, -25f, 0f, 0f)),
            new ComboStep("X-Cross", 3, 5, 5, 0.98, 2.9, 145.0, 0.95, 0.32, false, false, -36.0,
                    new WeaponPose(0.48f, 1.24f, 0.24f, -82f, -18f, -35f),
                    new WeaponPose(0.24f, 1.20f, 0.56f, -45f, -10f, -35f),
                    new WeaponPose(-0.46f, 1.08f, 0.54f, 88f, 14f, -35f),
                    new WeaponPose(0.26f, 1.14f, 0.34f, -10f, 0f, 0f))
    )),

    HAMMER(List.of(
            // 1段目: 横殴りバッシュ
            new ComboStep("Heavy Bash", 7, 6, 9, 1.15, 3.6, 145.0, 1.55, 0.24, false, false, -10.0,
                    new WeaponPose(0.58f, 1.24f, -0.05f, -105f, 0f, 85f),
                    new WeaponPose(0.36f, 1.18f, 0.48f, -65f, 0f, 85f),
                    new WeaponPose(-0.54f, 1.14f, 0.46f, 105f, 0f, 85f),
                    new WeaponPose(-0.34f, 1.14f, 0.30f, 40f, 0f, 25f)),
            // 2段目: 粉砕叩きつけ（超高体勢削り＋地面衝撃波）
            new ComboStep("Crush Slam", 9, 6, 10, 1.55, 3.8, 105.0, 2.20, 0.34, false, true, 90.0,
                    new WeaponPose(0.16f, 1.85f, -0.15f, 0f, -108f, 0f),
                    new WeaponPose(0.14f, 1.50f, 0.40f, 0f, -35f, 0f),
                    new WeaponPose(0.14f, 0.80f, 0.60f, 0f, 45f, 0f),
                    new WeaponPose(0.28f, 1.14f, 0.32f, -10f, 10f, 0f))
    )),

    SCYTHE(List.of(
            // 1段目: 低〜中段の広角水平刈り取り
            new ComboStep("Horizontal Reap", 6, 6, 8, 1.18, 4.3, 170.0, 1.15, 0.26, false, false, 6.0,
                    new WeaponPose(0.62f, 1.14f, -0.08f, -110f, 6f, 8f),
                    new WeaponPose(0.40f, 1.12f, 0.50f, -72f, 8f, 8f),
                    new WeaponPose(-0.60f, 1.10f, 0.48f, 118f, 10f, 8f),
                    new WeaponPose(-0.38f, 1.12f, 0.32f, 42f, 4f, 4f)),
            // 2段目: 逆水平深淵リープ
            new ComboStep("Abyss Reap", 6, 6, 8, 1.38, 4.4, 170.0, 1.30, 0.30, false, false, -8.0,
                    new WeaponPose(-0.56f, 1.06f, 0.16f, 102f, 10f, -12f),
                    new WeaponPose(-0.36f, 1.10f, 0.50f, 65f, 6f, -12f),
                    new WeaponPose(0.60f, 1.20f, 0.48f, -112f, -4f, -12f),
                    new WeaponPose(0.38f, 1.14f, 0.32f, -36f, 0f, 0f))
    ));

    // ガード・パリィ・弾かれ硬直の共通 3D ポーズ
    public static final WeaponPose GUARD_POSE =
            new WeaponPose(0.18f, 1.28f, 0.56f, -55f, -38f, 24f);
    public static final WeaponPose PARRY_DEFLECT_POSE =
            new WeaponPose(0.46f, 1.40f, 0.62f, 32f, -36f, -20f);
    public static final WeaponPose PARRIED_RECOIL_POSE =
            new WeaponPose(0.68f, 1.66f, 0.36f, 76f, -68f, -36f);
    public static final WeaponPose GUARD_CLASH_RECOIL_POSE =
            new WeaponPose(0.52f, 1.42f, 0.44f, 38f, -32f, -18f);

    private final List<ComboStep> steps;

    MeleeWeaponArchetype(List<ComboStep> steps) {
        this.steps = List.copyOf(steps);
    }

    public List<ComboStep> getSteps() {
        return steps;
    }

    public ComboStep getStep(int index) {
        return steps.get(Math.floorMod(index, steps.size()));
    }

    public int stepCount() {
        return steps.size();
    }

    /**
     * {@code CustomItem.getWeaponType()} の日本語武器種名からアーキタイプを解決する。
     * 弓・杖・その他非近接アイテムは {@code null} を返す。
     */
    public static MeleeWeaponArchetype fromWeaponType(String weaponType) {
        if (weaponType == null || weaponType.isBlank()) return null;
        return switch (weaponType.trim()) {
            case "剣", "マチェット" -> SWORD;
            case "大剣" -> GREATSWORD;
            case "斧" -> AXE;
            case "槍", "ハルバード" -> SPEAR;
            case "ダガー" -> DAGGER;
            case "メイス", "ハンマー" -> HAMMER;
            case "鎌" -> SCYTHE;
            default -> null;
        };
    }

    public record WeaponPose(
            float offsetX,
            float offsetY,
            float offsetZ,
            float yawDeg,
            float pitchDeg,
            float rollDeg
    ) {
    }

    public record ComboStep(
            String label,
            int windupTicks,
            int activeTicks,
            int recoveryTicks,
            double damageMultiplier,
            double reach,
            double arcDegrees,
            double staggerMultiplier,
            double stepForwardImpulse,
            boolean thrust,
            boolean groundShockwave,
            double slashRollDeg,
            WeaponPose windupPose,
            WeaponPose activeStartPose,
            WeaponPose activeEndPose,
            WeaponPose recoveryPose
    ) {
    }
}
