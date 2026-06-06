package com.ruskserver.deepwither_V2.modules.character.commands;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterMode;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterPersistenceException;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.CharacterStatus;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import com.ruskserver.deepwither_V2.modules.character.gui.CharacterCreateGui;
import com.ruskserver.deepwither_V2.modules.character.gui.CharacterSelectGui;
import com.ruskserver.deepwither_V2.modules.gui.GuiService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Command(name = "character", description = "キャラクターを管理します", aliases = {"char"})
public class CommandCharacter implements BasicCommand {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final GuiService guiService;

    @Inject
    public CommandCharacter(CharacterService characterService, CharacterNameTagService nameTagService, GuiService guiService) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
        this.guiService = guiService;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            openCharacterSelect(player);
            return;
        }

        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "create" -> {
                if (args.length == 1) {
                    openCharacterCreate(player);
                } else {
                    handleCreate(player, args);
                }
            }
            case "select" -> handleSelect(player, args);
            case "info" -> handleInfo(player);
            default -> showHelp(player);
        }
    }

    public void openCharacterSelect(Player player) {
        guiService.open(player, CharacterSelectGui.ID);
    }

    public void openCharacterCreate(Player player) {
        guiService.open(player, CharacterCreateGui.ID);
    }

    public void showCharacterList(Player player) {
        List<GameCharacter> characters = characterService.getCharacters(player.getUniqueId());
        Optional<GameCharacter> active = characterService.getActiveCharacter(player.getUniqueId());

        player.sendMessage(Component.text("===== キャラクター一覧 =====", NamedTextColor.GOLD));
        if (characters.isEmpty()) {
            player.sendMessage(Component.text("キャラクターがありません。/character create <名前> <standard|soft|true> で作成してください。", NamedTextColor.GRAY));
            return;
        }

        for (GameCharacter character : characters) {
            boolean isActive = active.map(GameCharacter::characterId).filter(character.characterId()::equals).isPresent();
            NamedTextColor statusColor = character.status() == CharacterStatus.ALIVE ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY;
            player.sendMessage(Component.text(isActive ? "* " : "  ", NamedTextColor.YELLOW)
                    .append(Component.text(character.name(), character.isSelectable() ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                    .append(Component.text(" [", NamedTextColor.GRAY))
                    .append(Component.text(character.mode().getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text(" / ", NamedTextColor.GRAY))
                    .append(Component.text(character.status().name(), statusColor))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text(character.characterId().toString().substring(0, 8), NamedTextColor.DARK_GRAY)));
        }
        player.sendMessage(Component.text("選択: /character select <名前またはID先頭>", NamedTextColor.GRAY));
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("使用法: /character create <名前> <standard|soft|true>", NamedTextColor.RED));
            return;
        }

        CharacterMode mode;
        try {
            mode = CharacterMode.parse(args[2]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("modeは standard / soft / true のいずれかを指定してください。", NamedTextColor.RED));
            return;
        }

        if (characterService.isSelectionLocked(player.getUniqueId())) {
            player.sendMessage(Component.text("死亡処理が完了するまでキャラクターを作成できません。", NamedTextColor.RED));
            return;
        }

        try {
            GameCharacter character = characterService.createCharacter(player.getUniqueId(), args[1], mode, false);
            if (!characterService.selectCharacter(player, character.characterId())) {
                player.sendMessage(Component.text("作成したキャラクターの選択に失敗しました。", NamedTextColor.RED));
                return;
            }
            nameTagService.refresh(player);
            player.sendMessage(Component.text("キャラクターを作成して選択しました: ", NamedTextColor.GREEN)
                    .append(Component.text(character.name(), NamedTextColor.YELLOW))
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(mode.getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text(")", NamedTextColor.GRAY)));
        } catch (CharacterPersistenceException e) {
            player.sendMessage(Component.text("キャラクターの作成に失敗しました。", NamedTextColor.RED));
        }
    }

    private void handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("使用法: /character select <名前またはID先頭>", NamedTextColor.RED));
            return;
        }

        Optional<GameCharacter> optional;
        try {
            optional = characterService.findOwnedCharacter(player.getUniqueId(), args[1]);
        } catch (CharacterPersistenceException e) {
            player.sendMessage(Component.text("キャラクターデータの読み込みに失敗しました。", NamedTextColor.RED));
            return;
        }
        if (optional.isEmpty()) {
            player.sendMessage(Component.text("キャラクターが見つかりません。", NamedTextColor.RED));
            return;
        }

        GameCharacter character = optional.get();
        if (!character.isSelectable()) {
            player.sendMessage(Component.text("死亡済みまたはアーカイブ済みのキャラクターは選択できません。", NamedTextColor.RED));
            return;
        }

        try {
            if (!characterService.selectCharacter(player, character.characterId())) {
                player.sendMessage(Component.text("キャラクターの選択に失敗しました。", NamedTextColor.RED));
                return;
            }
            nameTagService.refresh(player);
            player.sendMessage(Component.text("キャラクターを選択しました: ", NamedTextColor.GREEN)
                    .append(Component.text(character.name(), NamedTextColor.YELLOW)));
        } catch (CharacterPersistenceException e) {
            player.sendMessage(Component.text("キャラクターデータの保存に失敗しました。", NamedTextColor.RED));
        }
    }

    private void handleInfo(Player player) {
        Optional<GameCharacter> active;
        try {
            active = characterService.getActiveCharacter(player.getUniqueId());
        } catch (CharacterPersistenceException e) {
            player.sendMessage(Component.text("キャラクターデータの読み込みに失敗しました。", NamedTextColor.RED));
            return;
        }
        if (active.isEmpty()) {
            player.sendMessage(Component.text("アクティブキャラクターがありません。", NamedTextColor.RED));
            return;
        }

        GameCharacter character = active.get();
        player.sendMessage(Component.text("===== キャラクター情報 =====", NamedTextColor.GOLD));
        player.sendMessage(line("名前", character.name()));
        player.sendMessage(line("モード", character.mode().getDisplayName()));
        player.sendMessage(line("状態", character.status().name()));
        player.sendMessage(line("作成", DATE_FORMAT.format(Instant.ofEpochMilli(character.createdAt()))));
        if (character.diedAt() > 0) {
            player.sendMessage(line("死亡", DATE_FORMAT.format(Instant.ofEpochMilli(character.diedAt()))));
        }
    }

    private Component line(String label, String value) {
        return Component.text(label + ": ", NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.WHITE));
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("===== キャラクターコマンド =====", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/character list", NamedTextColor.YELLOW).append(Component.text(" - キャラクター選択GUI", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/character create", NamedTextColor.YELLOW).append(Component.text(" - キャラクター作成GUI", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/character create <名前> <standard|soft|true>", NamedTextColor.YELLOW).append(Component.text(" - 名前指定で作成", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/character select <名前またはID先頭>", NamedTextColor.YELLOW).append(Component.text(" - 選択", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/character info", NamedTextColor.YELLOW).append(Component.text(" - 現在のキャラ", NamedTextColor.GRAY)));
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(List.of("list", "create", "select", "info"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            suggestions.addAll(List.of("standard", "soft", "true"));
        }
        return suggestions;
    }
}
