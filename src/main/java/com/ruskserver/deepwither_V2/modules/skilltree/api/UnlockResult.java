package com.ruskserver.deepwither_V2.modules.skilltree.api;

import net.kyori.adventure.text.Component;

public final class UnlockResult {

    private final boolean success;
    private final Component message;

    private UnlockResult(boolean success, Component message) {
        this.success = success;
        this.message = message;
    }

    public static UnlockResult success(Component message) {
        return new UnlockResult(true, message);
    }

    public static UnlockResult fail(Component message) {
        return new UnlockResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public Component getMessage() {
        return message;
    }
}
