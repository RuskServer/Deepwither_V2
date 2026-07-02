package com.ruskserver.deepwither_V2.modules.dialogue.api;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public record DialogueChoice(
        String text,
        @Nullable String nextNodeId,
        @Nullable DialogueCondition condition,
        List<DialogueAction> actions
) {
    public DialogueChoice(String text, @Nullable String nextNodeId) {
        this(text, nextNodeId, null, List.of());
    }

    public DialogueChoice(String text, @Nullable String nextNodeId, @Nullable DialogueCondition condition) {
        this(text, nextNodeId, condition, List.of());
    }
}
