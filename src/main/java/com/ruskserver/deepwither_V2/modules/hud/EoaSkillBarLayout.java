package com.ruskserver.deepwither_V2.modules.hud;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fキー スキルキャストモード時の 4 スロット中央スキルバー (アイコン + CD ワイプ + 秒数表示) レイアウト計算。
 * <p>
 * コードポイント・寸法は {@code EoA_pack/tools/skill_icons.py} および
 * {@code EoA_pack/contents/assets/minecraft/shaders/include/eoa_text_hud.glsl} と厳密に一致させる。
 * 4 スロット (幅 18px, ピッチ 22px, 合計幅 84px) を 182px の中央 (x=49..133) に配置し、
 * シェーダー側で中央基準 1.45 倍 (約 26 GUI px/スロット) へ拡大描画する。
 */
public final class EoaSkillBarLayout {

    public static final int DISPLAY_SLOT_COUNT = 4;
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_PITCH = 22;     // 18px スロット + 4px ギャップ
    public static final int START_X = 49;        // (182 - (4*18 + 3*4)) / 2 = (182 - 84) / 2 = 49
    public static final int ICON_STEPS = 16;

    private static final int SPACE_POS = 0xE500;
    private static final int SPACE_NEG = 0xE510;
    private static final int SPACE_BITS = 9;

    private static final char SLOT_READY = 0xE800;
    private static final char SLOT_CD = 0xE801;
    private static final char SLOT_NOMANA = 0xE802;
    private static final char SLOT_CASTING = 0xE803;
    private static final char SLOT_EMPTY = 0xE804;
    private static final char SLOT_LOCKED = 0xE805;
    private static final char OVERLAY_NOMANA = 0xE806;
    private static final char CD_WIPE_BASE = 0xE810;
    private static final char KEY_BASE = 0xE820;
    private static final char CD_NUM_BASE = 0xE830;
    private static final char ICON_FALLBACK = 0xE84F;
    private static final char ICON_BASE = 0xE850;

    /** EoA_pack/tools/skill_icons.py の SKILL_IDS と同一順序 (全48スキル) */
    public static final List<String> SKILL_ICON_ORDER = List.of(
            // Fire (4)
            "fireball", "fire_nova", "flame_breath", "flame_pillar",
            // Ice (4)
            "ice_shard", "ice_spike", "frost_breath", "blizzard",
            // Lightning (4)
            "chain_lightning", "lightning_storm", "thunder_blast", "thunder_strike",
            // Arcane & Mobility (4)
            "arcane_bolt", "blink", "charge", "evade",
            // Holy / Heal / Support (12)
            "first_aid", "holy_light", "holy_nova", "holy_resurrection",
            "mass_heal", "divine_shield", "guardian_angel", "martyrdom",
            "purify", "renewal", "sacrificial_light", "sanctuary",
            // Warrior / Melee / Defense (12)
            "power_strike", "multi_slash", "whirlwind", "executioner",
            "hammer_slam", "seismic_stomp", "shockwave", "battle_cry",
            "colossus", "fortress", "shield_wall", "taunt",
            // Archer / Ranged (8)
            "power_shot", "triple_shot", "barrage", "endless_volley",
            "explosion_arrow", "focused_shot", "rain_of_arrows", "true_shot"
    );

    private static final Map<String, Character> SKILL_GLYPHS = buildSkillGlyphMap();

    private EoaSkillBarLayout() {
    }

    private static Map<String, Character> buildSkillGlyphMap() {
        Map<String, Character> map = new HashMap<>(SKILL_ICON_ORDER.size() * 2);
        for (int i = 0; i < SKILL_ICON_ORDER.size(); i++) {
            map.put(SKILL_ICON_ORDER.get(i), (char) (ICON_BASE + i));
        }
        return Map.copyOf(map);
    }

    public static char iconGlyphFor(String skillId) {
        if (skillId == null) return ICON_FALLBACK;
        return SKILL_GLYPHS.getOrDefault(skillId, ICON_FALLBACK);
    }

    public enum SlotState {
        EMPTY,
        LOCKED,
        READY,
        COOLDOWN,
        NO_MANA,
        CASTING
    }

    public record SkillSlotSnapshot(
            int keyNumber,
            String skillId,
            SlotState state,
            int cdWipeStep,
            String cdText
    ) {
        public static SkillSlotSnapshot empty(int keyNumber) {
            return new SkillSlotSnapshot(keyNumber, null, SlotState.EMPTY, 0, "");
        }

        public static SkillSlotSnapshot locked(int keyNumber, String skillId) {
            return new SkillSlotSnapshot(keyNumber, skillId, SlotState.LOCKED, 0, "");
        }

        public static SkillSlotSnapshot cooldown(int keyNumber, String skillId, long remainingMillis, long totalMillis) {
            double ratio = totalMillis > 0L
                    ? Math.max(0.0, Math.min(1.0, (double) remainingMillis / totalMillis))
                    : 1.0;
            int step = Math.max(1, Math.min(ICON_STEPS, (int) Math.round(ratio * ICON_STEPS)));
            String text = formatCooldown(remainingMillis);
            return new SkillSlotSnapshot(keyNumber, skillId, SlotState.COOLDOWN, step, text);
        }
    }

    public record SkillBarSnapshot(List<SkillSlotSnapshot> slots) {
    }

    public record RenderedSkillBar(String slotText, String cdText) {
    }

    public static String formatCooldown(long remainingMillis) {
        double sec = Math.max(0.1, remainingMillis / 1000.0);
        if (sec >= 9.95) {
            return Integer.toString((int) Math.ceil(sec));
        }
        return String.format(Locale.ROOT, "%.1f", sec);
    }

    public static RenderedSkillBar build(SkillBarSnapshot bar) {
        Cursor cursor = new Cursor();
        StringBuilder slotBuf = new StringBuilder(96);
        StringBuilder cdBuf = new StringBuilder(48);

        List<SkillSlotSnapshot> slots = bar.slots();
        int count = Math.min(DISPLAY_SLOT_COUNT, slots.size());

        // 1) スロット層 (中央配置 4 スロット: アイコン -> CDワイプ/マナ不足幕 -> 枠 -> 左上キー番号)
        for (int i = 0; i < DISPLAY_SLOT_COUNT; i++) {
            SkillSlotSnapshot slot = i < count ? slots.get(i) : SkillSlotSnapshot.empty(i + 1);
            int sx = START_X + i * SLOT_PITCH;
            int keyIdx = Math.max(0, Math.min(8, slot.keyNumber() - 1));

            if (slot.state() == SlotState.EMPTY) {
                cursor.moveTo(slotBuf, sx);
                cursor.emit(slotBuf, SLOT_EMPTY, SLOT_SIZE);
                cursor.moveTo(slotBuf, sx);
                cursor.emit(slotBuf, (char) (KEY_BASE + keyIdx), SLOT_SIZE);
                continue;
            }
            if (slot.state() == SlotState.LOCKED) {
                cursor.moveTo(slotBuf, sx);
                cursor.emit(slotBuf, SLOT_LOCKED, SLOT_SIZE);
                cursor.moveTo(slotBuf, sx);
                cursor.emit(slotBuf, (char) (KEY_BASE + keyIdx), SLOT_SIZE);
                continue;
            }

            cursor.moveTo(slotBuf, sx);
            cursor.emit(slotBuf, iconGlyphFor(slot.skillId()), SLOT_SIZE);

            switch (slot.state()) {
                case COOLDOWN -> {
                    int step = Math.max(1, Math.min(ICON_STEPS, slot.cdWipeStep()));
                    cursor.moveTo(slotBuf, sx);
                    cursor.emit(slotBuf, (char) (CD_WIPE_BASE + step - 1), SLOT_SIZE);
                    cursor.moveTo(slotBuf, sx);
                    cursor.emit(slotBuf, SLOT_CD, SLOT_SIZE);
                }
                case NO_MANA -> {
                    cursor.moveTo(slotBuf, sx);
                    cursor.emit(slotBuf, OVERLAY_NOMANA, SLOT_SIZE);
                    cursor.moveTo(slotBuf, sx);
                    cursor.emit(slotBuf, SLOT_NOMANA, SLOT_SIZE);
                }
                case CASTING -> {
                    cursor.moveTo(slotBuf, sx);
                    cursor.emit(slotBuf, SLOT_CASTING, SLOT_SIZE);
                }
                case READY -> {
                    cursor.moveTo(slotBuf, sx);
                    cursor.emit(slotBuf, SLOT_READY, SLOT_SIZE);
                }
                default -> {
                }
            }

            cursor.moveTo(slotBuf, sx);
            cursor.emit(slotBuf, (char) (KEY_BASE + keyIdx), SLOT_SIZE);
        }

        // 2) CD 秒数オーバーレイ層 (スロット下中央へ配置)
        for (int i = 0; i < count; i++) {
            SkillSlotSnapshot slot = slots.get(i);
            if (slot.state() != SlotState.COOLDOWN || slot.cdText() == null || slot.cdText().isEmpty()) {
                continue;
            }
            String text = slot.cdText();
            int textWidth = measureCdTextPixelWidth(text);
            int sx = START_X + i * SLOT_PITCH;
            int x = sx + (SLOT_SIZE - textWidth) / 2;
            for (int j = 0; j < text.length(); j++) {
                char ch = text.charAt(j);
                int idx = cdCharIndex(ch);
                if (idx < 0) continue;
                int imgW = cdCharImageWidth(ch);
                cursor.moveTo(cdBuf, x);
                cursor.emit(cdBuf, (char) (CD_NUM_BASE + idx), imgW);
                x += imgW - 1;
            }
        }

        // 全体の送り幅を厳密に 182 GUI px に揃える
        cursor.moveTo(cdBuf, EoaHudLayout.TOTAL_WIDTH);
        return new RenderedSkillBar(slotBuf.toString(), cdBuf.toString());
    }

    private static int measureCdTextPixelWidth(String text) {
        int sum = 0;
        for (int i = 0; i < text.length(); i++) {
            int imgW = cdCharImageWidth(text.charAt(i));
            if (imgW > 0) sum += imgW - 1;
        }
        return sum > 0 ? sum + 1 : 0;
    }

    private static int cdCharIndex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c == '.') return 10;
        return -1;
    }

    private static int cdCharImageWidth(char c) {
        if (c >= '0' && c <= '9') return 5;
        if (c == '.') return 3;
        return 0;
    }

    /** テスト検証用: 生成されたスキルバー文字列の総送り幅 (GUI px) を計算する */
    static int measureAdvance(String text) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= SPACE_POS && c < SPACE_POS + SPACE_BITS) {
                total += 1 << (c - SPACE_POS);
            } else if (c >= SPACE_NEG && c < SPACE_NEG + SPACE_BITS) {
                total -= 1 << (c - SPACE_NEG);
            } else if (c == SLOT_READY || c == SLOT_CD || c == SLOT_NOMANA || c == SLOT_CASTING
                    || c == SLOT_EMPTY || c == SLOT_LOCKED || c == OVERLAY_NOMANA
                    || (c >= CD_WIPE_BASE && c < CD_WIPE_BASE + ICON_STEPS)
                    || (c >= KEY_BASE && c < KEY_BASE + 9)
                    || c == ICON_FALLBACK
                    || (c >= ICON_BASE && c < ICON_BASE + SKILL_ICON_ORDER.size())) {
                total += SLOT_SIZE + 1;
            } else if (c >= CD_NUM_BASE && c < CD_NUM_BASE + 10) {
                total += 5 + 1;
            } else if (c == CD_NUM_BASE + 10) {
                total += 3 + 1;
            }
        }
        return total;
    }

    private static final class Cursor {
        private int x = 0;

        void moveTo(StringBuilder buf, int targetX) {
            int delta = targetX - x;
            if (delta != 0) {
                EoaHudLayout.appendSpace(buf, delta);
                x = targetX;
            }
        }

        void emit(StringBuilder buf, char glyph, int imageWidth) {
            buf.append(glyph);
            x += imageWidth + 1;
        }
    }
}
