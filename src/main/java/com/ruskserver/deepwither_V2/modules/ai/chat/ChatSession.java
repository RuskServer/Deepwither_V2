package com.ruskserver.deepwither_V2.modules.ai.chat;

import java.util.UUID;

public class ChatSession {

    private final UUID userId;
    private final StringBuilder conversation;

    public ChatSession(UUID userId) {
        this.userId = userId;
        this.conversation = new StringBuilder();
    }

    public UUID getUserId() {
        return userId;
    }

    public void appendUserMessage(String message) {
        conversation.append("ユーザー: ").append(message).append("\n");
    }

    public void appendAssistantMessage(String message) {
        conversation.append("アシスタント: ").append(message).append("\n");
    }

    public String getRecentHistory(int maxLines) {
        String[] lines = conversation.toString().split("\n");
        int start = Math.max(0, lines.length - maxLines);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    public void clear() {
        conversation.setLength(0);
    }
}
