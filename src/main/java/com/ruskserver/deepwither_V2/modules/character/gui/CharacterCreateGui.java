package com.ruskserver.deepwither_V2.modules.character.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterMode;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterPersistenceException;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import com.ruskserver.deepwither_V2.modules.gui.GuiClickContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiItemBuilder;
import com.ruskserver.deepwither_V2.modules.gui.GuiRenderContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CharacterCreateGui implements GuiView {
    public static final String ID = "character_create";
    private static final int SIZE = 27;
    private static final int STANDARD_SLOT = 11;
    private static final int SOFT_HARDCORE_SLOT = 13;
    private static final int TRUE_HARDCORE_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final Deepwither_V2 plugin;

    @Inject
    public CharacterCreateGui(CharacterService characterService, CharacterNameTagService nameTagService, Deepwither_V2 plugin) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getTitle(Player player, GuiContext context) {
        return Component.text("キャラクター作成", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public int getSize(Player player, GuiContext context) {
        return SIZE;
    }

    @Override
    public void render(GuiRenderContext context) {
        for (int i = 0; i < SIZE; i++) {
            context.setItem(i, GuiItemBuilder.background(Material.GRAY_STAINED_GLASS_PANE));
        }

        context.setItem(STANDARD_SLOT, GuiItemBuilder.of(Material.PLAYER_HEAD)
                .name(Component.text("スタンダード", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(
                        Component.text("従来通り死亡後にリスポーンします。", NamedTextColor.GRAY),
                        Component.text("所持金と信用度はソフトHCと共有です。", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("クリックして作成", NamedTextColor.YELLOW)
                )
                .build());
        context.setItem(SOFT_HARDCORE_SLOT, GuiItemBuilder.of(Material.TOTEM_OF_UNDYING)
                .name(Component.text("ソフトHC", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(
                        Component.text("死亡するとキャラ選択へ戻ります。", NamedTextColor.GRAY),
                        Component.text("名前には [SHC] prefix が付きます。", NamedTextColor.GRAY),
                        Component.text("所持金と信用度はスタンダードと共有です。", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("クリックして作成", NamedTextColor.YELLOW)
                )
                .build());
        context.setItem(TRUE_HARDCORE_SLOT, GuiItemBuilder.of(Material.WITHER_SKELETON_SKULL)
                .name(Component.text("真HC", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(
                        Component.text("死亡するとキャラ選択へ戻ります。", NamedTextColor.GRAY),
                        Component.text("名前には [THC] prefix が付きます。", NamedTextColor.GRAY),
                        Component.text("所持金と信用度はキャラ個別です。", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("クリックして作成", NamedTextColor.YELLOW)
                )
                .build());
        context.setItem(BACK_SLOT, GuiItemBuilder.of(Material.BARRIER)
                .name(Component.text("戻る", NamedTextColor.RED))
                .build());
    }

    @Override
    public void onClick(GuiClickContext context) {
        CharacterMode mode = switch (context.slot()) {
            case STANDARD_SLOT -> CharacterMode.STANDARD;
            case SOFT_HARDCORE_SLOT -> CharacterMode.SOFT_HARDCORE;
            case TRUE_HARDCORE_SLOT -> CharacterMode.TRUE_HARDCORE;
            default -> null;
        };

        if (context.slot() == BACK_SLOT) {
            context.open(CharacterSelectGui.ID);
            return;
        }

        if (mode == null) {
            return;
        }

        Player player = context.player();
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        CharacterMode selectedMode = mode;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        player.sendMessage(Component.text("キャラクターを作成しています...", NamedTextColor.GRAY));
        characterService.saveCharacterState(player);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                GameCharacter character = characterService.createGeneratedCharacter(playerId, playerName, selectedMode);
                characterService.loadAndApplyCharacterDataAsync(playerId, character.characterId(), () -> {
                    Player online = plugin.getServer().getPlayer(playerId);
                    if (online == null || !online.isOnline()) {
                        return;
                    }
                    nameTagService.refresh(online, character.mode());
                    online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
                    online.sendMessage(Component.text("キャラクターを作成して選択しました: ", NamedTextColor.GREEN)
                            .append(Component.text(character.name(), NamedTextColor.YELLOW)));
                    online.closeInventory();
                });
            } catch (CharacterPersistenceException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player online = plugin.getServer().getPlayer(playerId);
                    if (online != null && online.isOnline()) {
                        online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                        online.sendMessage(Component.text("キャラクターの作成に失敗しました。", NamedTextColor.RED));
                    }
                });
            }
        });
    }
}
