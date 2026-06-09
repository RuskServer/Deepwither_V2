package com.ruskserver.deepwither_V2.modules.character.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterPersistenceException;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import com.ruskserver.deepwither_V2.modules.character.commands.CommandCharacter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class HardcoreDeathListener implements Listener, PlayerLifecycleTask {
    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final CommandCharacter commandCharacter;
    private final Deepwither_V2 plugin;
    private final Logger logger;
    private final Set<UUID> processingDeaths = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingSelectionGui = ConcurrentHashMap.newKeySet();
    private final Set<UUID> respawnedBeforeDeathSave = ConcurrentHashMap.newKeySet();

    @Inject
    public HardcoreDeathListener(CharacterService characterService, CharacterNameTagService nameTagService,
                                 CommandCharacter commandCharacter, Deepwither_V2 plugin, Logger logger) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
        this.commandCharacter = commandCharacter;
        this.plugin = plugin;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Optional<GameCharacter> deathSnapshot = characterService.getCachedActiveCharacter(playerId);
        if (deathSnapshot.isEmpty() || !deathSnapshot.get().mode().isHardcore()) {
            return;
        }
        if (!characterService.lockSelection(playerId)) {
            return;
        }
        processingDeaths.add(playerId);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> processHardcoreDeathAsync(playerId, deathSnapshot.get()));
    }

    private void processHardcoreDeathAsync(UUID playerId, GameCharacter deathSnapshot) {
        try {
            Optional<GameCharacter> deadCharacter = characterService.markCharacterDead(playerId, deathSnapshot);
            plugin.getServer().getScheduler().runTask(plugin, () -> finishDeathProcessing(playerId, deadCharacter));
        } catch (CharacterPersistenceException e) {
            logger.log(Level.SEVERE, "Failed to process hardcore death for " + playerId, e);
            plugin.getServer().getScheduler().runTask(plugin, () -> failDeathProcessing(playerId));
        }
    }

    private void finishDeathProcessing(UUID playerId, Optional<GameCharacter> deadCharacter) {
        processingDeaths.remove(playerId);
        characterService.unlockSelection(playerId);
        if (deadCharacter.isEmpty()) {
            respawnedBeforeDeathSave.remove(playerId);
            return;
        }

        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            // 真HCキャラのVault残高を没収
            characterService.confiscateEconomy(player, deadCharacter.get());
            sendDeathNotice(player, deadCharacter.get());
            nameTagService.clear(player);
            if (respawnedBeforeDeathSave.remove(playerId)) {
                commandCharacter.openCharacterSelect(player);
            } else {
                pendingSelectionGui.add(playerId);
            }
        } else {
            clearPendingState(playerId);
        }
    }

    private void failDeathProcessing(UUID playerId) {
        clearPendingState(playerId);
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(net.kyori.adventure.text.Component.text("キャラクター死亡処理の保存に失敗しました。管理者に連絡してください。", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (pendingSelectionGui.remove(playerId)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> commandCharacter.openCharacterSelect(event.getPlayer()));
        } else if (processingDeaths.contains(playerId)) {
            respawnedBeforeDeathSave.add(playerId);
        }
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
        clearPendingState(context.playerId());
        return CompletableFuture.completedFuture(null);
    }

    private void clearPendingState(UUID playerId) {
        processingDeaths.remove(playerId);
        pendingSelectionGui.remove(playerId);
        respawnedBeforeDeathSave.remove(playerId);
        characterService.unlockSelection(playerId);
    }

    private void sendDeathNotice(Player player, GameCharacter deadCharacter) {
        player.sendMessage(net.kyori.adventure.text.Component.text("キャラクターが死亡しました: ", NamedTextColor.RED)
                .append(net.kyori.adventure.text.Component.text(deadCharacter.name(), NamedTextColor.YELLOW))
                .append(net.kyori.adventure.text.Component.text(" (", NamedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text(deadCharacter.mode().getDisplayName(), NamedTextColor.GOLD))
                .append(net.kyori.adventure.text.Component.text(")", NamedTextColor.GRAY)));
        player.sendMessage(net.kyori.adventure.text.Component.text("リスポーン後にキャラクター選択GUIを開きます。", NamedTextColor.GRAY));
    }
}
