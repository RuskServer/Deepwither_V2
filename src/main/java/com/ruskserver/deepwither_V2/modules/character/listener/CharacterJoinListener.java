package com.ruskserver.deepwither_V2.modules.character.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterMode;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import com.ruskserver.deepwither_V2.modules.character.gui.CharacterSelectGui;
import com.ruskserver.deepwither_V2.modules.gui.GuiService;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.scheduler.BukkitTask;

@Component
public class CharacterJoinListener implements PlayerLifecycleTask {
    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final GuiService guiService;
    private final Deepwither_V2 plugin;
    private final Logger logger;

    @Inject
    public CharacterJoinListener(CharacterService characterService, CharacterNameTagService nameTagService,
                                 GuiService guiService, Deepwither_V2 plugin, Logger logger) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
        this.guiService = guiService;
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.JOIN);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.CHARACTER;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        UUID playerId = context.playerId();
        return context.supplyAsync(() -> characterService.ensureLegacyCharacter(playerId, context.playerName()))
                .thenCompose(activeCharacter -> applyCharacterState(context, activeCharacter))
                .handle((ignored, error) -> {
                    if (error == null) {
                        return null;
                    }

                    Throwable cause = error instanceof CompletionException && error.getCause() != null
                            ? error.getCause()
                            : error;
                    logger.log(Level.SEVERE, "Failed to ensure character for joining player " + playerId, cause);
                    context.runSync(() -> context.player().ifPresent(player ->
                            player.sendMessage(net.kyori.adventure.text.Component.text(
                                    "キャラクターデータの読み込みに失敗しました。管理者に連絡してください。",
                                    NamedTextColor.RED))));
                    throw new CompletionException(cause);
                });
    }

    private CompletableFuture<Void> applyCharacterState(PlayerLifecycleContext context, Optional<GameCharacter> activeCharacter) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        UUID playerId = context.playerId();

        if (activeCharacter.isPresent()) {
            GameCharacter character = activeCharacter.get();
            AtomicReference<BukkitTask> disconnectWatch = new AtomicReference<>();
            final int timeoutTicks = 1200;
            disconnectWatch.set(plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
                private int elapsedTicks = 0;

                @Override
                public void run() {
                    if (future.isDone()) {
                        disconnectWatch.get().cancel();
                        return;
                    }
                    elapsedTicks++;
                    if (elapsedTicks >= timeoutTicks) {
                        future.completeExceptionally(new TimeoutException("Character loading timed out for player " + playerId));
                        disconnectWatch.get().cancel();
                        return;
                    }
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player == null || !player.isOnline()) {
                        future.complete(null);
                        disconnectWatch.get().cancel();
                    }
                }
            }, 1L, 1L));
            characterService.loadAndApplyCharacterDataAsync(playerId, character.characterId(), () -> {
                try {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        nameTagService.refresh(player, character.mode());
                    }
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    BukkitTask task = disconnectWatch.get();
                    if (task != null) {
                        task.cancel();
                    }
                }
            });
            return future;
        }

        context.runSync(() -> context.player().ifPresent(player -> {
            nameTagService.refresh(player, CharacterMode.STANDARD);
            player.sendMessage(net.kyori.adventure.text.Component.text(
                    "アクティブキャラクターがありません。キャラクターを選択または作成してください。",
                    NamedTextColor.YELLOW));
            guiService.open(player, CharacterSelectGui.ID);
        })).whenComplete((ignored, error) -> {
            if (error != null) {
                future.completeExceptionally(error);
            } else {
                future.complete(null);
            }
        });
        return future;
    }
}
