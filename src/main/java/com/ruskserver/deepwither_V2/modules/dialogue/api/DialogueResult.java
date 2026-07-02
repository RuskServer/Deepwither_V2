package com.ruskserver.deepwither_V2.modules.dialogue.api;

import org.jetbrains.annotations.Nullable;
import java.util.Map;

public record DialogueResult(
        String dialogueId,
        @Nullable String endNodeId,
        @Nullable String selectedChoiceText,
        Map<String, String> flags
) {
    public boolean completed() {
        return endNodeId != null;
    }
}
