package com.ruskserver.deepwither_V2.modules.skill.api;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class SkillTag {

    private SkillTag() {
    }

    public enum Role {
        ATTACK("攻撃", NamedTextColor.RED),
        CONTROL("妨害", NamedTextColor.YELLOW),
        DEFENSE("防御", NamedTextColor.BLUE),
        SUPPORT("支援", NamedTextColor.GREEN),
        UTILITY("機能", NamedTextColor.LIGHT_PURPLE);

        private final String label;
        private final TextColor color;

        Role(String label, TextColor color) {
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public TextColor color() {
            return color;
        }
    }

    public enum Tactic {
        BURST("瞬発", NamedTextColor.GOLD),
        DISPLACE("位置操作", NamedTextColor.DARK_AQUA),
        ANTI_TANK("対重装", NamedTextColor.DARK_RED),
        MOBILITY("機動力", NamedTextColor.AQUA),
        DISPEL("解除", NamedTextColor.WHITE);

        private final String label;
        private final TextColor color;

        Tactic(String label, TextColor color) {
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public TextColor color() {
            return color;
        }
    }

    public enum Scaling {
        PHYSICAL("物理", NamedTextColor.GOLD),
        MAGICAL("魔法", NamedTextColor.LIGHT_PURPLE),
        HYBRID("複合", NamedTextColor.YELLOW),
        CDR_HEAVY("高回転", NamedTextColor.AQUA);

        private final String label;
        private final TextColor color;

        Scaling(String label, TextColor color) {
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public TextColor color() {
            return color;
        }
    }

    public enum Constraint {
        CHANNELING("詠唱", NamedTextColor.DARK_PURPLE),
        HIGH_COST("高燃費", NamedTextColor.DARK_RED),
        LONG_CD("長大CD", NamedTextColor.GRAY);

        private final String label;
        private final TextColor color;

        Constraint(String label, TextColor color) {
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        public TextColor color() {
            return color;
        }
    }
}
