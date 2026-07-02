package com.ruskserver.deepwither_V2.modules.dialogue.api;

import java.util.List;

public record DialogueNode(
        String id,
        SpeakerType speaker,
        String text,
        List<DialogueAction> actions,
        List<DialogueChoice> choices
) {
    public DialogueNode(String id, SpeakerType speaker, String text) {
        this(id, speaker, text, List.of(), List.of());
    }

    public boolean isPlayerNode() {
        return speaker == SpeakerType.PLAYER;
    }

    public boolean hasChoices() {
        return !choices.isEmpty();
    }
}
