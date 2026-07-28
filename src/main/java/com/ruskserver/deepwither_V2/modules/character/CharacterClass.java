package com.ruskserver.deepwither_V2.modules.character;

import org.bukkit.Material;

public enum CharacterClass {
    WARRIOR(
            "戦士",
            Material.IRON_SWORD,
            "近接戦闘と耐久力に優れた前衛クラス。",
            "最大HPが10%増加する。",
            "剣技・機動力・防御を伸ばして最前線を支える。"
    ),
    MAGE(
            "魔術師",
            Material.BLAZE_ROD,
            "強力な魔法攻撃を扱う遠距離クラス。",
            "最大マナが10%増加する。",
            "属性魔法と範囲攻撃を伸ばして敵を一掃する。"
    ),
    ARCHER(
            "弓使い",
            Material.BOW,
            "距離を保ちながら攻撃する機動型クラス。",
            "移動速度が10%増加する。",
            "射撃・罠・回避を伸ばして安全圏から戦う。"
    ),
    HOLY(
            "神聖",
            Material.GOLDEN_APPLE,
            "回復と防護で味方を支える支援クラス。",
            "魔法防御力が10%増加する。",
            "治癒・加護・神聖攻撃を伸ばして戦線を維持する。"
    ),
    UNKNOWN("未設定", Material.BARRIER, "", "", "");

    private final String displayName;
    private final Material icon;
    private final String description;
    private final String classBonus;
    private final String playstyle;

    CharacterClass(String displayName, Material icon, String description, String classBonus, String playstyle) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.classBonus = classBonus;
        this.playstyle = playstyle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public String getClassBonus() {
        return classBonus;
    }

    public String getPlaystyle() {
        return playstyle;
    }
}
