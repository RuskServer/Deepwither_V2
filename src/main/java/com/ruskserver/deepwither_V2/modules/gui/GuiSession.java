package com.ruskserver.deepwither_V2.modules.gui;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class GuiSession {

    private static final int MAX_HISTORY = 16;

    private final UUID playerId;
    private final Deque<GuiHistoryEntry> history = new ArrayDeque<>();
    private String currentGuiId;
    private GuiContext currentContext;
    private UUID sessionToken;
    private boolean transitioning;
    private Instant lastAccessAt;

    public GuiSession(UUID playerId) {
        this.playerId = playerId;
        this.currentContext = GuiContext.EMPTY;
        touch();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCurrentGuiId() {
        return currentGuiId;
    }

    public GuiContext getCurrentContext() {
        return currentContext;
    }

    public UUID getSessionToken() {
        return sessionToken;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public void setTransitioning(boolean transitioning) {
        this.transitioning = transitioning;
        touch();
    }

    public boolean isExpired(Instant now, Duration ttl) {
        return lastAccessAt.plus(ttl).isBefore(now);
    }

    public void setCurrent(String guiId, GuiContext context, UUID sessionToken) {
        this.currentGuiId = guiId;
        this.currentContext = context == null ? GuiContext.EMPTY : context;
        this.sessionToken = sessionToken;
        touch();
    }

    public void pushCurrentToHistory() {
        if (currentGuiId == null) return;
        GuiHistoryEntry last = history.peekLast();
        if (last != null && last.guiId().equals(currentGuiId) && last.context().values().equals(currentContext.values())) {
            return;
        }
        history.addLast(new GuiHistoryEntry(currentGuiId, currentContext));
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    public GuiHistoryEntry popHistory() {
        touch();
        return history.pollLast();
    }

    public void touch() {
        this.lastAccessAt = Instant.now();
    }
}
