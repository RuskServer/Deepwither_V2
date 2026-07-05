package com.ruskserver.deepwither_V2.modules.character.gui;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import org.bukkit.entity.Player;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CharacterCreateGui {
    public static final String ID = CharacterDialogService.CREATE_ID;

    private final CharacterDialogService dialogService;

    @Inject
    public CharacterCreateGui(CharacterDialogService dialogService) {
        this.dialogService = dialogService;
    }

    public void open(Player player) {
        dialogService.openCreate(player);
    }
}
