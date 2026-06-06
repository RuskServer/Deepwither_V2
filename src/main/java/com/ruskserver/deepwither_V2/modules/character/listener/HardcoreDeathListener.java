package com.ruskserver.deepwither_V2.modules.character.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterPersistenceException;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import com.ruskserver.deepwither_V2.modules.character.commands.CommandCharacter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class HardcoreDeathListener implements Listener {
    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final CommandCharacter commandCharacter;
    private final Deepwither_V2 plugin;
    private final Logger logger;
    private final Set<UUID> pendingSelectionGui = ConcurrentHashMap.newKeySet();

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
        try {
            characterService.markActiveCharacterDead(event.getPlayer()).ifPresent(deadCharacter -> {
                sendDeathNotice(event.getPlayer(), deadCharacter);
                nameTagService.clear(event.getPlayer());
                pendingSelectionGui.add(event.getPlayer().getUniqueId());
            });
        } catch (CharacterPersistenceException e) {
            logger.log(Level.SEVERE, "Failed to process hardcore death for " + event.getPlayer().getUniqueId(), e);
            event.getPlayer().sendMessage(net.kyori.adventure.text.Component.text("キャラクター死亡処理の保存に失敗しました。管理者に連絡してください。", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!pendingSelectionGui.remove(event.getPlayer().getUniqueId())) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> commandCharacter.openCharacterSelect(event.getPlayer()));
    }

    private void sendDeathNotice(org.bukkit.entity.Player player, GameCharacter deadCharacter) {
        player.sendMessage(net.kyori.adventure.text.Component.text("キャラクターが死亡しました: ", NamedTextColor.RED)
                .append(net.kyori.adventure.text.Component.text(deadCharacter.name(), NamedTextColor.YELLOW))
                .append(net.kyori.adventure.text.Component.text(" (", NamedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text(deadCharacter.mode().getDisplayName(), NamedTextColor.GOLD))
                .append(net.kyori.adventure.text.Component.text(")", NamedTextColor.GRAY)));
        player.sendMessage(net.kyori.adventure.text.Component.text("リスポーン後にキャラクター選択GUIを開きます。", NamedTextColor.GRAY));
    }
}
