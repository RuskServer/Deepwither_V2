package com.ruskserver.deepwither_V2.modules.dialogue.command;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueGraph;
import com.ruskserver.deepwither_V2.modules.dialogue.service.DialogueService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Command(name = "dialogue", aliases = {"dlg", "talk"}, description = "会話システム管理コマンド")
public class DialogueCommand implements BasicCommand {

    private final DialogueService dialogueService;

    @Inject
    public DialogueCommand(DialogueService dialogueService) {
        this.dialogueService = dialogueService;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 0) {
            stack.getSender().sendMessage(Component.text("使用法: /dialogue start <npcName> | end | reload", NamedTextColor.RED));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(stack, args);
            case "end" -> handleEnd(stack);
            case "reload" -> handleReload(stack);
            default -> stack.getSender().sendMessage(Component.text("不明なサブコマンド: " + args[0], NamedTextColor.RED));
        }
    }

    private void handleStart(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("プレイヤーのみ実行可能です", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("使用法: /dialogue start <npcName>", NamedTextColor.RED));
            return;
        }

        String npcName = args[1];
        if (!dialogueService.hasDialogue(npcName)) {
            player.sendMessage(Component.text("NPC '" + npcName + "' に会話が登録されていません", NamedTextColor.RED));
            return;
        }

        dialogueService.startNpcDialogue(player, npcName)
                .thenAccept(result -> {
                    if (result.completed()) {
                        player.sendMessage(Component.text("会話が終了しました", NamedTextColor.GRAY));
                    }
                });
    }

    private void handleEnd(CommandSourceStack stack) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("プレイヤーのみ実行可能です", NamedTextColor.RED));
            return;
        }

        dialogueService.endDialogue(player);
        player.sendMessage(Component.text("会話を強制終了しました", NamedTextColor.YELLOW));
    }

    private void handleReload(CommandSourceStack stack) {
        stack.getSender().sendMessage(Component.text("会話リロード機能は未実装です", NamedTextColor.YELLOW));
    }
}
