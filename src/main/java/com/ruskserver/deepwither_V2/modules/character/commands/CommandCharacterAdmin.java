package com.ruskserver.deepwither_V2.modules.character.commands;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Command(name = "characteradmin", description = "キャラクター管理者コマンド", aliases = {"charadmin"})
public class CommandCharacterAdmin implements BasicCommand {
    private final CharacterService characterService;

    @Inject
    public CommandCharacterAdmin(CharacterService characterService) {
        this.characterService = characterService;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!stack.getSender().hasPermission("deepwither.character.admin")) {
            stack.getSender().sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("revive")) {
            stack.getSender().sendMessage(Component.text("使用法: /characteradmin revive <player> <character>", NamedTextColor.RED));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null) {
            stack.getSender().sendMessage(Component.text("プレイヤーが見つかりません。", NamedTextColor.RED));
            return;
        }

        if (characterService.reviveCharacter(target.getUniqueId(), args[2])) {
            stack.getSender().sendMessage(Component.text("キャラクターを復活しました。", NamedTextColor.GREEN));
        } else {
            stack.getSender().sendMessage(Component.text("死亡済みキャラクターが見つからないか、復活できません。", NamedTextColor.RED));
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("revive");
        }
        return suggestions;
    }
}
