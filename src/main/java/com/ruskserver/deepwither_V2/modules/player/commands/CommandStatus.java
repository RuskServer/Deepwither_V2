package com.ruskserver.deepwither_V2.modules.player.commands;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.player.gui.StatusDialogService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
@Command(name = "status", description = "現在のステータス詳細を表示します", aliases = {})
public class CommandStatus implements BasicCommand {

    private final StatusDialogService statusDialogService;

    @Inject
    public CommandStatus(StatusDialogService statusDialogService) {
        this.statusDialogService = statusDialogService;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("プレイヤーのみ実行できます。");
            return;
        }

        statusDialogService.open(player);
    }
}
