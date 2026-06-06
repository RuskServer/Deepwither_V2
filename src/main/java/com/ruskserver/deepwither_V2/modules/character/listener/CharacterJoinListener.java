package com.ruskserver.deepwither_V2.modules.character.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterMode;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterPersistenceException;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class CharacterJoinListener implements Listener {
    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final Deepwither_V2 plugin;
    private final Logger logger;

    @Inject
    public CharacterJoinListener(CharacterService characterService, CharacterNameTagService nameTagService,
                                 Deepwither_V2 plugin, Logger logger) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
        this.plugin = plugin;
        this.logger = logger;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> ensureCharacterAsync(playerId, playerName));
    }

    private void ensureCharacterAsync(UUID playerId, String playerName) {
        try {
            GameCharacter character = characterService.ensureLegacyCharacter(playerId, playerName);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    nameTagService.refresh(player, character.isSelectable() ? character.mode() : CharacterMode.STANDARD);
                }
            });
        } catch (CharacterPersistenceException e) {
            logger.log(Level.SEVERE, "Failed to ensure character for joining player " + playerId, e);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(net.kyori.adventure.text.Component.text("キャラクターデータの読み込みに失敗しました。管理者に連絡してください。", NamedTextColor.RED));
                }
            });
        }
    }
}
