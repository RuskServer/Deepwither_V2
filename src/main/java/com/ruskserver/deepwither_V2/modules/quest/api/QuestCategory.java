package com.ruskserver.deepwither_V2.modules.quest.api;

public enum QuestCategory {
    MAIN("§6[メイン]"),
    TUTORIAL("§b[チュートリアル]"),
    DAILY("§a[デイリー]"),
    SIDE("§e[サブ]");

    private final String prefix;

    QuestCategory(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
