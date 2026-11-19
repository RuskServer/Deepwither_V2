package com.ruskserver.deepwither_V2.modules.hud;

import com.ruskserver.deepwither_V2.modules.hud.EoaHudLayout.HudSnapshot;
import com.ruskserver.deepwither_V2.modules.hud.EoaHudLayout.RenderedLayers;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

/**
 * {@link EoaHudLayout} が組み立てた文字レイヤーへ {@code eoa:hud} フォントと
 * {@code eoa_text_hud.glsl} 用のマーカー色を付与して Adventure {@link Component} を生成する。
 */
public final class EoaHudRenderer {

    public static final Key FONT = Key.key("eoa", "hud");

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

    private EoaHudRenderer() {
    }

    public static Component render(HudSnapshot snapshot) {
        RenderedLayers layers = EoaHudLayout.build(snapshot);
        TextComponent.Builder root = Component.text();
        root.append(Component.text(layers.barText(), BAR_STYLE));
        root.append(Component.text(layers.numText(), NUM_STYLE));
        return root.build();
    }
}
