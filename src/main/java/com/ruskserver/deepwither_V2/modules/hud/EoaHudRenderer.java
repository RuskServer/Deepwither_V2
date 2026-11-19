package com.ruskserver.deepwither_V2.modules.hud;

import com.ruskserver.deepwither_V2.modules.hud.EoaHudLayout.HudSnapshot;
import com.ruskserver.deepwither_V2.modules.hud.EoaHudLayout.RenderedLayers;
import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.RenderedSkillBar;
import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.SkillBarSnapshot;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

/**
 * {@link EoaHudLayout} と {@link EoaSkillBarLayout} が組み立てた文字レイヤーへ
 * フォントと {@code eoa_text_hud.glsl} 用のマーカー色を付与して Adventure {@link Component} を生成する。
 */
public final class EoaHudRenderer {

    public static final Key FONT = Key.key("eoa", "hud");
    public static final Key SKILL_FONT = Key.key("eoa", "skill");

    /** eoa_text_hud.glsl がホットバー直上へ再配置するためのマーカー色 (影なし) */
    public static final Style BAR_STYLE = Style.empty()
            .font(FONT)
            .color(TextColor.color(233, 232, 231))
            .shadowColor(ShadowColor.none());

    /** eoa_text_hud.glsl がバー直上 (数値行) へ再配置するためのマーカー色 (影なし) */
    public static final Style NUM_STYLE = Style.empty()
            .font(FONT)
            .color(TextColor.color(233, 232, 232))
            .shadowColor(ShadowColor.none());

    /** eoa_text_hud.glsl が Fキー スキルバー (9スロット) を配置するためのマーカー色 */
    public static final Style SKILL_BAR_STYLE = Style.empty()
            .font(SKILL_FONT)
            .color(TextColor.color(233, 232, 233))
            .shadowColor(ShadowColor.none());

    /** eoa_text_hud.glsl がスキル CD 秒数をスロット下中央へ配置するためのマーカー色 */
    public static final Style SKILL_CD_STYLE = Style.empty()
            .font(SKILL_FONT)
            .color(TextColor.color(233, 232, 234))
            .shadowColor(ShadowColor.none());

    private static final String RESET_CURSOR_TO_ZERO = buildResetSpace();

    private EoaHudRenderer() {
    }

    private static String buildResetSpace() {
        StringBuilder sb = new StringBuilder();
        EoaHudLayout.appendSpace(sb, -EoaHudLayout.TOTAL_WIDTH);
        return sb.toString();
    }

    public static Component render(HudSnapshot snapshot) {
        return render(snapshot, null);
    }

    public static Component render(HudSnapshot snapshot, SkillBarSnapshot skillBar) {
        RenderedLayers layers = EoaHudLayout.build(snapshot);
        TextComponent.Builder root = Component.text();
        root.append(Component.text(layers.barText(), BAR_STYLE));
        root.append(Component.text(layers.numText(), NUM_STYLE));
        if (skillBar != null) {
            RenderedSkillBar renderedSkill = EoaSkillBarLayout.build(skillBar);
            // HP/MP レイヤー終端 (x=182) から x=0 へ戻して 9 スロットを重ねる (総送り幅は 182 のまま)
            root.append(Component.text(RESET_CURSOR_TO_ZERO, BAR_STYLE));
            root.append(Component.text(renderedSkill.slotText(), SKILL_BAR_STYLE));
            root.append(Component.text(renderedSkill.cdText(), SKILL_CD_STYLE));
        }
        return root.build();
    }
}
