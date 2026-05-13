package com.ruskserver.deepwither_V2.modules.artifact.commands;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.artifact.gui.ArtifactGui;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Component
@Command(name = "artifact", description = "アーティファクト装備GUIを開きます")
public class CommandArtifact implements BasicCommand {

    private final ArtifactGui gui;

    @Inject
    public CommandArtifact(ArtifactGui gui) {
        this.gui = gui;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("このコマンドはプレイヤーのみ実行可能です。");
            return;
        }
        gui.openGui(player);
    }
}
