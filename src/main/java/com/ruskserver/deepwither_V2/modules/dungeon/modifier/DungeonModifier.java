package com.ruskserver.deepwither_V2.modules.dungeon.modifier;

import net.kyori.adventure.text.format.TextColor;

public enum DungeonModifier {

    BLOODLUST(
            "bloodlust", "血狂",
            "敵の攻撃力が2倍になる代わりに、チェストの数と品質が上昇する。",
            0xFF4444,
            2.0, 1.0, 1.0, 1.5, 1.5, 1.0, 0
    ),
    FRAILTY(
            "frailty", "虚弱",
            "敵の攻撃力が半分になる代わりに、チェストの数と品質が低下する。",
            0xAAAAAA,
            0.5, 1.0, 1.0, 0.5, 0.5, 1.0, 0
    ),
    SWARM(
            "swarm", "群集",
            "敵のスポーン数が2倍になる代わりに、チェストの数が増加する。",
            0x55FF55,
            1.0, 1.5, 2.0, 1.5, 1.0, 1.0, 0
    ),
    SWIFT(
            "swift", "迅雷",
            "制限時間が半分になる代わりに、チェストの品質が大幅に向上する。",
            0xFFFF55,
            1.0, 1.0, 1.0, 1.0, 2.0, 0.5, 0
    ),
    BULWARK(
            "bulwark", "鉄壁",
            "敵のHPが2倍になる代わりに、パーティのライフが増加する。",
            0x55AAFF,
            1.3, 2.0, 1.0, 0.7, 1.0, 1.0, 2
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final TextColor color;
    private final double mobAtkMultiplier;
    private final double mobHpMultiplier;
    private final double mobCountMultiplier;
    private final double lootQtyMultiplier;
    private final double lootQualMultiplier;
    private final double timeLimitMultiplier;
    private final int extraLives;

    DungeonModifier(String id, String displayName, String description, int color,
                    double mobAtk, double mobHp, double mobCount,
                    double lootQty, double lootQual,
                    double timeLimit, int extraLives) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.color = TextColor.color(color);
        this.mobAtkMultiplier = mobAtk;
        this.mobHpMultiplier = mobHp;
        this.mobCountMultiplier = mobCount;
        this.lootQtyMultiplier = lootQty;
        this.lootQualMultiplier = lootQual;
        this.timeLimitMultiplier = timeLimit;
        this.extraLives = extraLives;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public TextColor color() { return color; }
    public double mobAtkMultiplier() { return mobAtkMultiplier; }
    public double mobHpMultiplier() { return mobHpMultiplier; }
    public double mobCountMultiplier() { return mobCountMultiplier; }
    public double lootQtyMultiplier() { return lootQtyMultiplier; }
    public double lootQualMultiplier() { return lootQualMultiplier; }
    public double timeLimitMultiplier() { return timeLimitMultiplier; }
    public int extraLives() { return extraLives; }

    public static DungeonModifier byId(String id) {
        for (var m : values()) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }
}
