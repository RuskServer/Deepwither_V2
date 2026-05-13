package com.ruskserver.deepwither_V2.modules.artifact;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;

/**
 * アーティファクトのセット効果を定義する列挙型。
 */
public enum ArtifactSetType {
    ABYSS_PULSATION("深淵の鼓動"),
    CELESTIAL_RESONANCE("星界の共鳴"),
    FAULT_LINE("断層の輪"),
    ASTRAL_STEEL_GUARD("星盾の守護"),
    LUNAR_SKIRMISHER("月駆の遊撃兵"),
    ETERNAL_HEARTS("永遠の心臓");

    private final String displayName;

    ArtifactSetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Component> getLoreLines() {
        return switch (this) {
            case ABYSS_PULSATION -> List.of(
                    Component.text("2セット: 最大HP +10%, 魔法ダメージ +8%", NamedTextColor.AQUA),
                    Component.text("3セット: 魔法被弾時、8秒CDで完全遮断障壁を展開", NamedTextColor.LIGHT_PURPLE),
                    Component.text("         障壁発動時、周囲3ブロックをノックバックし盲目を付与", NamedTextColor.GRAY)
            );
            case CELESTIAL_RESONANCE -> List.of(
                    Component.text("2セット: 最大マナ +60, 魔法攻撃力 +15%", NamedTextColor.AQUA),
                    Component.text("3セット: 魔法攻撃時、マナ75%以上なら魔法ダメージの5%を確定ダメージ化", NamedTextColor.LIGHT_PURPLE),
                    Component.text("         魔法クリティカル発動時、25%で追撃", NamedTextColor.GRAY)
            );
            case FAULT_LINE -> List.of(
                    Component.text("2セット: 物理攻撃力 +12, クリティカル率 +2%", NamedTextColor.AQUA),
                    Component.text("3セット: クリティカル時、10秒CDで対象の防御を20%無視", NamedTextColor.LIGHT_PURPLE),
                    Component.text("         クリティカル後3秒間、移動速度 +10%", NamedTextColor.GRAY)
            );
            case ASTRAL_STEEL_GUARD -> List.of(
                    Component.text("2セット: 防御力 +25, HP自動回復強化", NamedTextColor.AQUA),
                    Component.text("3セット: 物理被弾時、受けたダメージの10%をマナへ変換", NamedTextColor.LIGHT_PURPLE),
                    Component.text("         HP30%以下で60秒CDの再生IIを付与", NamedTextColor.GRAY),
                    Component.text("         魔法防御力 +15", NamedTextColor.GRAY)
            );
            case LUNAR_SKIRMISHER -> List.of(
                    Component.text("2セット: 移動速度 +2%, クリティカルダメージ +10%", NamedTextColor.AQUA),
                    Component.text("3セット: 移動時、5%の確率で完全回避を付与", NamedTextColor.LIGHT_PURPLE),
                    Component.text("         ダッシュ中の初撃に魔法追撃を付与", NamedTextColor.GRAY)
            );
            case ETERNAL_HEARTS -> List.of(
                    Component.text("2セット: 最大HP +25%, 防御力 +10%", NamedTextColor.AQUA),
                    Component.text("3セット: 致死ダメージを1回だけHP1で耐える", NamedTextColor.LIGHT_PURPLE),
                    Component.text("         発動時、周囲の敵を強く弾き飛ばす (300秒CD)", NamedTextColor.GRAY)
            );
        };
    }
}
