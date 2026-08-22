package com.ruskserver.deepwither_V2.modules.skill.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.modules.dialogue.service.DialogueService;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillCastService;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillSessionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class SkillInputListener implements Listener, PlayerLifecycleTask {

    private final SkillSessionService sessionService;
    private final SkillCastService castService;
    private final DialogueService dialogueService;

    @Inject
    public SkillInputListener(SkillSessionService sessionService, SkillCastService castService, DialogueService dialogueService) {
        this.sessionService = sessionService;
        this.castService = castService;
        this.dialogueService = dialogueService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() || dialogueService.isInDialogue(player)) return;

        event.setCancelled(true);
        sessionService.toggleSkillMode(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHotbarChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (dialogueService.isInDialogue(player)) return;
        if (!sessionService.isInSkillMode(player)) return;

        event.setCancelled(true);
        if (castService.isCasting(player)) {
            return;
        }
        sessionService.castSlot(player, event.getNewSlot());
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.QUIT);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.CLEANUP;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> context.player().ifPresent(player -> {
            if (sessionService.isInSkillMode(player)) {
                sessionService.exitSkillMode(player);
            }
        }));
    }
}
