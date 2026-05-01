package com.ruskserver.deepwither_V2.modules.skilltree.commands;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.skilltree.gui.SkillTreeGui;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Component
@Command(name = "skilltree", description = "スキルツリーGUIを開きます", aliases = {"stree"})
public class CommandSkillTree implements BasicCommand {

    private final SkillTreeGui gui;

    @Inject
    public CommandSkillTree(SkillTreeGui gui) {
        this.gui = gui;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("プレイヤーのみ実行可能です。");
            return;
        }
        gui.openSelectorOrFirst(player);
    }
}
