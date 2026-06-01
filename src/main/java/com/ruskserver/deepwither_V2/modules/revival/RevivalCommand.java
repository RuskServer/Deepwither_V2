package com.ruskserver.deepwither_V2.modules.revival;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Command(name = "suicide", description = "ダウン状態から強制的にリスポーンします")
public class RevivalCommand implements BasicCommand {

    private final RevivalManager revivalManager;

    @Inject
    public RevivalCommand(RevivalManager revivalManager) {
        this.revivalManager = revivalManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        CommandSender sender = commandSourceStack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cこのコマンドはプレイヤーのみ使用できます。"));
            return;
        }

        if (!revivalManager.isDowned(player)) {
            player.sendMessage(Component.text("§c現在ダウン状態ではありません。", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("§e>> セーフゾーンにリスポーンします...", NamedTextColor.YELLOW));
        revivalManager.forceDeath(player);
    }
}
