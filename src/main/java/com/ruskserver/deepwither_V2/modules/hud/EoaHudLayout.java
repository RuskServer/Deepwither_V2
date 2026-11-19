package com.ruskserver.deepwither_V2.modules.hud;

/**
 * Echoes of Aether 自前 HUD (体力・マジックシールド・バリア・マナ) の文字配置と送り幅計算。
 * <p>
 * コードポイント・寸法は {@code EoA_pack/tools/hud_bars.py} および
 * {@code EoA_pack/contents/assets/minecraft/shaders/include/eoa_text_hud.glsl} と厳密に一致させる。
 * 全体の送り幅を常に {@link #TOTAL_WIDTH} (182 GUI px = ホットバー全幅) へ揃えることで、
 * BossBar の中央揃えだけでホットバー直上に 1px のズレもなく整列する。
 */
public final class EoaHudLayout {

    /** ホットバー全幅と一致する全体幅 (GUI px) */
    public static final int TOTAL_WIDTH = 182;
    public static final int UNIT_WIDTH = 82;
    public static final int CENTER_GAP = 18;
    public static final int ICON_SIZE = 13;
    public static final int ICON_GAP = 3;
    public static final int BAR_WIDTH = 66;
    public static final int WELL_STEPS = 62;
    public static final int LOW_HP_MAX_STEPS = 16;

    private static final int DIGIT_IMAGE_WIDTH = 5;
    private static final int DIGIT_STEP = 4; // 1px 縁を重ねてコンパクトに並べる

    // 空白グリフ: +2^i が U+E500+i、-2^i が U+E510+i (i = 0..8)
    private static final int SPACE_POS = 0xE500;
    private static final int SPACE_NEG = 0xE510;
    private static final int SPACE_BITS = 9;

    // バー・フレーム・数値グリフ (hud_bars.py と同一)
    private static final char HP_FRAME = 0xE600;
    private static final char HP_FRAME_LOW = 0xE601;
    private static final char MP_FRAME = 0xE603;
    private static final char HP_FILL_BASE = 0xE610;
    private static final char HP_LOW_BASE = 0xE660;
    private static final char BARRIER_BASE = 0xE680;
    private static final char MP_FILL_BASE = 0xE6D0;
    private static final char ES_FILL_BASE = 0xE760;

    private static final char NUM_HP_BASE = 0xE720;
    private static final char NUM_LOW_BASE = 0xE730;
    private static final char NUM_BAR_BASE = 0xE740;
    private static final char NUM_MP_BASE = 0xE750;
    private static final char NUM_ES_BASE = 0xE7A0;

    private EoaHudLayout() {
    }

    /** HUD の 1 フレーム分の表示状態 (整数丸め済みスナップショットで差分判定に使用) */
    public record HudSnapshot(
            int hp,
            int maxHp,
            int mp,
            int maxMp,
            int barrier,
            int magicShield,
            int maxMagicShield
    ) {
        public static HudSnapshot of(
                double hp,
                double maxHp,
                double mp,
                double maxMp,
                double barrier,
                double magicShield,
                double maxMagicShield
        ) {
            int safeMaxHp = Math.max(1, (int) Math.round(maxHp));
            int safeHp = Math.max(0, Math.min(safeMaxHp, (int) Math.ceil(Math.max(0.0, hp))));
            int safeMaxMp = Math.max(1, (int) Math.round(maxMp));
            int safeMp = Math.max(0, Math.min(safeMaxMp, (int) Math.floor(Math.max(0.0, mp))));
            int safeBarrier = Math.max(0, (int) Math.round(barrier));
            int safeMaxEs = Math.max(0, (int) Math.round(maxMagicShield));
            int safeEs = Math.max(0, Math.min(safeMaxEs, (int) Math.round(magicShield)));
            return new HudSnapshot(safeHp, safeMaxHp, safeMp, safeMaxMp, safeBarrier, safeEs, safeMaxEs);
        }
    }

    /** バー層と数値層の生文字列ペア */
    public record RenderedLayers(String barText, String numText) {
    }

    public static RenderedLayers build(HudSnapshot s) {
        double hpRatio = clamp01((double) s.hp() / s.maxHp());
        double mpRatio = clamp01((double) s.mp() / s.maxMp());
        boolean hasEs = s.magicShield() > 0 && s.maxMagicShield() > 0;
        boolean lowHp = hpRatio <= 0.25;

        int hpStep = toSteps(hpRatio);
        int mpStep = toSteps(mpRatio);
        int barrierStep = s.barrier() > 0 ? Math.max(1, toSteps((double) s.barrier() / s.maxHp())) : 0;
        int esStep = hasEs ? Math.max(1, toSteps((double) s.magicShield() / s.maxMagicShield())) : 0;

        Cursor cursor = new Cursor();
        StringBuilder barBuf = new StringBuilder(64);

        // 左: HP フレーム + 塗り + バリア + マジックシールド
        cursor.moveTo(barBuf, 0);
        cursor.emit(barBuf, (lowHp && !hasEs) ? HP_FRAME_LOW : HP_FRAME, UNIT_WIDTH);

        if (hpStep > 0) {
            cursor.moveTo(barBuf, 0);
            char fill = (lowHp && hpStep <= LOW_HP_MAX_STEPS)
                    ? (char) (HP_LOW_BASE + hpStep - 1)
                    : (char) (HP_FILL_BASE + hpStep - 1);
            cursor.emit(barBuf, fill, UNIT_WIDTH);
        }
        if (barrierStep > 0) {
            cursor.moveTo(barBuf, 0);
            cursor.emit(barBuf, (char) (BARRIER_BASE + barrierStep - 1), UNIT_WIDTH);
        }
        if (esStep > 0) {
            cursor.moveTo(barBuf, 0);
            cursor.emit(barBuf, (char) (ES_FILL_BASE + esStep - 1), UNIT_WIDTH);
        }

        // 右: マナフレーム + 塗り
        int mpX = UNIT_WIDTH + CENTER_GAP;
        cursor.moveTo(barBuf, mpX);
        cursor.emit(barBuf, MP_FRAME, UNIT_WIDTH);
        if (mpStep > 0) {
            cursor.moveTo(barBuf, mpX);
            cursor.emit(barBuf, (char) (MP_FILL_BASE + mpStep - 1), UNIT_WIDTH);
        }

        // 上段: 数値オーバーレイ (バー直上)
        StringBuilder numBuf = new StringBuilder(64);
        String hpText = s.hp() + "/" + s.maxHp();
        char hpNumBase = (lowHp && !hasEs) ? NUM_LOW_BASE : NUM_HP_BASE;
        int numX = ICON_SIZE + ICON_GAP + 1;
        numX = emitNumberText(cursor, numBuf, numX, hpText, hpNumBase);
        if (hasEs) {
            emitNumberText(cursor, numBuf, numX + 2, "+" + s.magicShield(), NUM_ES_BASE);
        } else if (s.barrier() > 0) {
            emitNumberText(cursor, numBuf, numX + 2, "+" + s.barrier(), NUM_BAR_BASE);
        }

        String mpText = s.mp() + "/" + s.maxMp();
        int mpTextWidth = mpText.length() * DIGIT_STEP + 1;
        int mpNumX = mpX + BAR_WIDTH - 1 - mpTextWidth;
        emitNumberText(cursor, numBuf, mpNumX, mpText, NUM_MP_BASE);

        // 全体の送り幅を厳密に TOTAL_WIDTH (182px) に揃える
        cursor.moveTo(numBuf, TOTAL_WIDTH);

        return new RenderedLayers(barBuf.toString(), numBuf.toString());
    }

    private static int emitNumberText(Cursor cursor, StringBuilder buf, int startX, String text, char base) {
        int x = startX;
        for (int i = 0; i < text.length(); i++) {
            int idx = digitIndex(text.charAt(i));
            if (idx < 0) continue;
            cursor.moveTo(buf, x);
            cursor.emit(buf, (char) (base + idx), DIGIT_IMAGE_WIDTH);
            x += DIGIT_STEP;
        }
        return x;
    }

    private static int digitIndex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c == '/') return 10;
        if (c == '+') return 11;
        return -1;
    }

    private static int toSteps(double ratio) {
        return (int) Math.round(clamp01(ratio) * WELL_STEPS);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    static void appendSpace(StringBuilder buf, int delta) {
        if (delta == 0) return;
        int base = delta > 0 ? SPACE_POS : SPACE_NEG;
        int remaining = Math.abs(delta);
        for (int bit = SPACE_BITS - 1; bit >= 0; bit--) {
            int step = 1 << bit;
            while (remaining >= step) {
                buf.append((char) (base + bit));
                remaining -= step;
            }
        }
    }

    /** テスト検証用: 生成された文字列の総送り幅 (GUI px) を計算する */
    static int measureAdvance(String text) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= SPACE_POS && c < SPACE_POS + SPACE_BITS) {
                total += 1 << (c - SPACE_POS);
            } else if (c >= SPACE_NEG && c < SPACE_NEG + SPACE_BITS) {
                total -= 1 << (c - SPACE_NEG);
            } else if (c == HP_FRAME || c == HP_FRAME_LOW || c == MP_FRAME
                    || (c >= HP_FILL_BASE && c < HP_FILL_BASE + WELL_STEPS)
                    || (c >= HP_LOW_BASE && c < HP_LOW_BASE + LOW_HP_MAX_STEPS)
                    || (c >= BARRIER_BASE && c < BARRIER_BASE + WELL_STEPS)
                    || (c >= MP_FILL_BASE && c < MP_FILL_BASE + WELL_STEPS)
                    || (c >= ES_FILL_BASE && c < ES_FILL_BASE + WELL_STEPS)) {
                total += UNIT_WIDTH + 1;
            } else if ((c >= NUM_HP_BASE && c < NUM_HP_BASE + 12)
                    || (c >= NUM_LOW_BASE && c < NUM_LOW_BASE + 12)
                    || (c >= NUM_BAR_BASE && c < NUM_BAR_BASE + 12)
                    || (c >= NUM_MP_BASE && c < NUM_MP_BASE + 12)
                    || (c >= NUM_ES_BASE && c < NUM_ES_BASE + 12)) {
                total += DIGIT_IMAGE_WIDTH + 1;
            }
        }
        return total;
    }

    private static final class Cursor {
        private int x = 0;

        void moveTo(StringBuilder buf, int targetX) {
            int delta = targetX - x;
            if (delta != 0) {
                appendSpace(buf, delta);
                x = targetX;
            }
        }

        void emit(StringBuilder buf, char glyph, int imageWidth) {
            buf.append(glyph);
            x += imageWidth + 1; // クライアントは右端 alpha=1 の列 + 1 だけ進める
        }
    }
}
