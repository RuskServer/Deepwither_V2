package com.ruskserver.deepwither_V2.modules.skill.service;

import org.bukkit.Input;

public record PlayerInputSnapshot(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        boolean sneak,
        boolean sprint
) {

    private static final PlayerInputSnapshot EMPTY = new PlayerInputSnapshot(false, false, false, false, false, false, false);

    public static PlayerInputSnapshot empty() {
        return EMPTY;
    }

    public static PlayerInputSnapshot from(Input input) {
        if (input == null) {
            return empty();
        }
        return new PlayerInputSnapshot(
                input.isForward(),
                input.isBackward(),
                input.isLeft(),
                input.isRight(),
                input.isJump(),
                input.isSneak(),
                input.isSprint()
        );
    }

    public boolean hasMovementInput() {
        return forward || backward || left || right;
    }
}
