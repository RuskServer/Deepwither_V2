package com.ruskserver.deepwither_V2.modules.dialogue.api;

@FunctionalInterface
public interface DialogueAction {
    void execute(DialogueContext context);
}
