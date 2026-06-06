package com.ruskserver.deepwither_V2.modules.character.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@Component
public class CharacterJoinListener implements Listener {
    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;

    @Inject
    public CharacterJoinListener(CharacterService characterService, CharacterNameTagService nameTagService) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        characterService.ensureLegacyCharacter(event.getPlayer());
        nameTagService.refresh(event.getPlayer());
    }
}
