package com.ruskserver.deepwither_V2.modules.dialogue.api;

@FunctionalInterface
public interface DialogueCondition {
    boolean test(DialogueContext context);
}
