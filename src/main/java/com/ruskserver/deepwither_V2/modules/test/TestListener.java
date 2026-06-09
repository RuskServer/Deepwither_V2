package com.ruskserver.deepwither_V2.modules.test;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class TestListener implements PlayerLifecycleTask {

    private final TestService testService;

    @Inject
    public TestListener(TestService testService) {
        this.testService = testService;
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.JOIN);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.GUI;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> context.player().ifPresent(player -> {
            // Test that the injected service works when a player joins
            testService.doSomething();
            player.sendMessage("§aDIコンテナによるリスナーの自動登録と依存関係の注入が正常に機能しています！");
        }));
    }
}
