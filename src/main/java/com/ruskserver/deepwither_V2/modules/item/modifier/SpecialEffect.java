package com.ruskserver.deepwither_V2.modules.item.modifier;

public enum SpecialEffect {

    LIFESTEAL("生命吸収", "与ダメージの3%をHPとして吸収する"),
    THORNS("棘の反撃", "被ダメージの5%を反射する"),
    MANA_SIPHON("魔力吸収", "攻撃時5%の確率でマナを10回復する"),
    FORTIFY("堅牢", "被弾時10%の確率で3秒間防御力+20%（クール5秒）"),
    HASTE("軽量化", "攻撃速度が10%上昇する"),
    BERSERK("怒涛", "HP50%以下で攻撃力が20%上昇する");

    private final String displayName;
    private final String description;

    SpecialEffect(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
