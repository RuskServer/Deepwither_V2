package com.ruskserver.deepwither_V2.modules.skill.api;

import net.kyori.adventure.text.Component;

public final class CastResult {

    private static final CastResult SUCCESS = new CastResult(true, null);
    private static final CastResult FAIL = new CastResult(false, null);

    private final boolean success;
    private final Component message;

    private CastResult(boolean success, Component message) {
        this.success = success;
        this.message = message;
    }

    public static CastResult success() {
        return SUCCESS;
    }

    public static CastResult fail() {
        return FAIL;
    }

    public static CastResult fail(Component message) {
        return new CastResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public Component getMessage() {
        return message;
    }
}
