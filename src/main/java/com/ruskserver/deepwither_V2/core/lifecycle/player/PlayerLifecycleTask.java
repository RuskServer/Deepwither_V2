package com.ruskserver.deepwither_V2.core.lifecycle.player;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface PlayerLifecycleTask {

    Set<PlayerLifecycleEventType> eventTypes();

    PlayerLifecyclePhase phase();

    default int order() {
        return 0;
    }

    CompletableFuture<Void> run(PlayerLifecycleContext context);
}
