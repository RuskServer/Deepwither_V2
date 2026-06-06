package com.ruskserver.deepwither_V2.modules.character.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import com.ruskserver.deepwither_V2.modules.character.commands.CommandCharacter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@Component
public class HardcoreDeathListener implements Listener {
    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final CommandCharacter commandCharacter;
    private final Deepwither_V2 plugin;

    @Inject
    public HardcoreDeathListener(CharacterService characterService, CharacterNameTagService nameTagService, CommandCharacter commandCharacter, Deepwither_V2 plugin) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
        this.commandCharacter = commandCharacter;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        characterService.markActiveCharacterDead(event.getPlayer()).ifPresent(deadCharacter -> {
            sendDeathNotice(event.getPlayer(), deadCharacter);
            nameTagService.clear(event.getPlayer());
            plugin.getServer().getScheduler().runTask(plugin, () -> commandCharacter.openCharacterSelect(event.getPlayer()));
        });
    }

    private void sendDeathNotice(org.bukkit.entity.Player player, GameCharacter deadCharacter) {
        player.sendMessage(net.kyori.adventure.text.Component.text("キャラクターが死亡しました: ", NamedTextColor.RED)
                .append(net.kyori.adventure.text.Component.text(deadCharacter.name(), NamedTextColor.YELLOW))
                .append(net.kyori.adventure.text.Component.text(" (", NamedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text(deadCharacter.mode().getDisplayName(), NamedTextColor.GOLD))
                .append(net.kyori.adventure.text.Component.text(")", NamedTextColor.GRAY)));
        player.sendMessage(net.kyori.adventure.text.Component.text("キャラクター選択GUIから別キャラを選択するか、新規作成してください。", NamedTextColor.GRAY));
    }
}
