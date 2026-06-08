package com.ruskserver.deepwither_V2.core.lifecycle.player;

public enum PlayerLifecyclePhase {
    EARLY(0),
    CHARACTER(100),
    DATA_APPLIED(200),
    INVENTORY_ITEMS(300),
    UI_ITEMS(400),
    STATS(500),
    GUI(600),
    CLEANUP(700);

    private final int order;

    PlayerLifecyclePhase(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
