package com.ruskserver.deepwither_V2.modules.player.commands;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.gui.AttributeGui;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * プレイヤーのレベルと属性に関するコマンド群。
 */
public class PlayerCommands {

    /**
     * 経験値を付与するテスト用コマンド。
     * 使用例: /addexp 500
     */
    @Component
    @Command(name = "addexp", description = "プレイヤーに経験値を付与します（管理者用）")
    public static class AddExpCommand implements BasicCommand {

        private final PlayerManager playerManager;

        @Inject
        public AddExpCommand(PlayerManager playerManager) {
            this.playerManager = playerManager;
        }

        @Override
        public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
            if (!(stack.getSender() instanceof Player player)) {
                stack.getSender().sendMessage("プレイヤーのみ実行可能です。");
                return;
            }

            if (args.length < 1) {
                player.sendMessage(net.kyori.adventure.text.Component.text("使用方法: /addexp <量>", NamedTextColor.RED));
                return;
            }

            try {
                int amount = Integer.parseInt(args[0]);
                if (amount <= 0) {
                    player.sendMessage(net.kyori.adventure.text.Component.text("1以上の数値を入力してください。", NamedTextColor.RED));
                    return;
                }
                playerManager.addExp(player, amount);
            } catch (NumberFormatException e) {
                player.sendMessage(net.kyori.adventure.text.Component.text("数値を入力してください。", NamedTextColor.RED));
            }
        }

        @Override
        public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
            if (args.length == 1) {
                return java.util.Arrays.asList("100", "500", "1000", "5000");
            }
            return Collections.emptyList();
        }
    }

    /**
     * 属性割り振りGUIを開くコマンド。
     * 使用例: /status
     */
    @Component
    @Command(name = "attribute", description = "ステータス割り振りGUIを開きます", aliases = {"att"})
    public static class StatusCommand implements BasicCommand {

        private final AttributeGui gui;

        @Inject
        public StatusCommand(AttributeGui gui) {
            this.gui = gui;
        }

        @Override
        public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
            if (!(stack.getSender() instanceof Player player)) {
                stack.getSender().sendMessage("プレイヤーのみ実行可能です。");
                return;
            }
            gui.open(player);
        }
    }
}
