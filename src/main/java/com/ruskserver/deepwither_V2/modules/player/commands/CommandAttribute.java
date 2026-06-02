package com.ruskserver.deepwither_V2.modules.player.commands;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.gui.GuiService;
import com.ruskserver.deepwither_V2.modules.player.gui.AttributeGui;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 属性割り振りGUIを開くコマンド。
 * 使用例: /attribute または /att
 */
@Component
@Command(name = "attribute", description = "ステータス割り振りGUIを開きます", aliases = {"att"})
public class CommandAttribute implements BasicCommand {

    private final GuiService guiService;

    @Inject
    public CommandAttribute(GuiService guiService) {
        this.guiService = guiService;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("プレイヤーのみ実行可能です。");
            return;
        }
        guiService.open(player, AttributeGui.ID);
    }
}
