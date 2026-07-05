package com.ruskserver.deepwither_V2.modules.character.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterMode;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterPersistenceException;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CharacterDialogService {
    public static final String SELECT_ID = "character_select";
    public static final String CREATE_ID = "character_create";

    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final Deepwither_V2 plugin;

    @Inject
    public CharacterDialogService(CharacterService characterService, CharacterNameTagService nameTagService, Deepwither_V2 plugin) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
        this.plugin = plugin;
    }

    public void openSelect(Player player) {
        List<GameCharacter> characters = characterService.getCachedCharacters(player.getUniqueId());
        Optional<GameCharacter> active = characterService.getCachedActiveCharacter(player.getUniqueId());
        UUID activeId = active.map(GameCharacter::characterId).orElse(null);

        List<ActionButton> buttons = new ArrayList<>();
        for (GameCharacter c : characters) {
            if (!c.isSelectable()) continue;
            UUID charId = c.characterId();
            boolean isActive = charId.equals(activeId);
            buttons.add(ActionButton.builder(
                Component.text(c.name(), isActive ? NamedTextColor.GOLD : NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, isActive)
            )
                .tooltip(Component.text("モード: " + c.mode().getDisplayName(), NamedTextColor.GRAY))
                .width(150)
                .action(DialogAction.customClick((view, audience) -> {
                    selectCharacter(player, charId);
                }, ClickCallback.Options.builder().uses(1).build()))
                .build());
        }

        buttons.add(ActionButton.builder(Component.text("新規作成", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true))
            .tooltip(Component.text("新しいキャラクターを作成します", NamedTextColor.GRAY))
            .width(150)
            .action(DialogAction.customClick((view, audience) -> {
                openCreate(player);
            }, ClickCallback.Options.builder().uses(1).build()))
            .build());

        Dialog dialog = Dialog.create(factory -> factory.empty()
            .base(DialogBase.builder(Component.text("キャラクター選択"))
                .body(List.of(DialogBody.plainMessage(
                    Component.text("選択するキャラクターをクリックしてください。", NamedTextColor.GRAY)
                )))
                .build())
            .type(DialogType.multiAction(buttons).build()));

        player.showDialog(dialog);
    }

    public void openCreate(Player player) {
        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(ActionButton.builder(Component.text("スタンダード", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true))
            .tooltip(Component.text("従来通り死亡後にリスポーンします。所持金と信用度はソフトHCと共有です。", NamedTextColor.GRAY))
            .width(150)
            .action(DialogAction.customClick((view, audience) -> {
                createCharacter(player, CharacterMode.STANDARD);
            }, ClickCallback.Options.builder().uses(1).build()))
            .build());

        buttons.add(ActionButton.builder(Component.text("ソフトHC", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
            .tooltip(Component.text("死亡するとキャラ選択へ戻ります。名前には [SHC] prefix が付きます。", NamedTextColor.GRAY))
            .width(150)
            .action(DialogAction.customClick((view, audience) -> {
                createCharacter(player, CharacterMode.SOFT_HARDCORE);
            }, ClickCallback.Options.builder().uses(1).build()))
            .build());

        buttons.add(ActionButton.builder(Component.text("真HC", NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
            .tooltip(Component.text("死亡するとキャラ選択へ戻ります。名前には [THC] prefix が付きます。所持金と信用度はキャラ個別です。", NamedTextColor.GRAY))
            .width(150)
            .action(DialogAction.customClick((view, audience) -> {
                createCharacter(player, CharacterMode.TRUE_HARDCORE);
            }, ClickCallback.Options.builder().uses(1).build()))
            .build());

        Dialog dialog = Dialog.create(factory -> factory.empty()
            .base(DialogBase.builder(Component.text("キャラクター作成"))
                .body(List.of(DialogBody.plainMessage(
                    Component.text("作成するモードを選択してください。", NamedTextColor.GRAY)
                )))
                .build())
            .type(DialogType.multiAction(buttons).build()));

        player.showDialog(dialog);
    }

    private void selectCharacter(Player player, UUID characterId) {
        player.sendMessage(Component.text("キャラクターを選択しています...", NamedTextColor.GRAY));
        characterService.switchCharacterAsync(player, characterId,
                () -> {
                    Player online = plugin.getServer().getPlayer(player.getUniqueId());
                    if (online == null || !online.isOnline()) return;
                    nameTagService.refresh(online, characterService.getCachedActiveCharacter(online.getUniqueId())
                            .map(GameCharacter::mode).orElse(CharacterMode.STANDARD));
                    online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                    online.sendMessage(Component.text("キャラクターを選択しました。", NamedTextColor.GREEN));
                },
                () -> {
                    Player online = plugin.getServer().getPlayer(player.getUniqueId());
                    if (online == null || !online.isOnline()) return;
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                    online.sendMessage(Component.text("キャラクターの選択に失敗しました。", NamedTextColor.RED));
                });
    }

    private void createCharacter(Player player, CharacterMode mode) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        player.sendMessage(Component.text("キャラクターを作成しています...", NamedTextColor.GRAY));
        characterService.saveCharacterState(player);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                GameCharacter character = characterService.createGeneratedCharacter(playerId, playerName, mode);
                characterService.loadAndApplyCharacterDataAsync(playerId, character.characterId(), () -> {
                    Player online = plugin.getServer().getPlayer(playerId);
                    if (online == null || !online.isOnline()) return;
                    nameTagService.refresh(online, character.mode());
                    online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
                    online.sendMessage(Component.text("キャラクターを作成して選択しました: ", NamedTextColor.GREEN)
                            .append(Component.text(character.name(), NamedTextColor.YELLOW)));
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
