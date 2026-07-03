package com.ruskserver.deepwither_V2.modules.dungeon.portal;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Command(
        name = "questportal",
        description = "クエストダンジョンポータル管理",
        aliases = {"qp"}
)
public class CommandQuestPortal implements BasicCommand {

    private final PortalLocationRepository repository;

    @Inject
    public CommandQuestPortal(PortalLocationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(stack);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(stack, args);
            case "remove" -> handleRemove(stack, args);
            case "list" -> handleList(stack);
            case "tp" -> handleTp(stack, args);
            default -> sendHelp(stack);
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list", "tp").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("tp"))) {
            return repository.getAll().stream()
                    .map(PortalLocation::id)
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    private void handleAdd(CommandSourceStack stack, String[] args) {
        if (args.length < 3) {
            stack.getSender().sendMessage(Component.text("使用法: /questportal add <id> <dungeonId>", NamedTextColor.RED));
            return;
        }
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        String dungeonId = args[2];
        var loc = player.getLocation();
        repository.add(new PortalLocation(id, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), dungeonId));
        stack.getSender().sendMessage(Component.text("ポータル '" + id + "' を登録しました。", NamedTextColor.GREEN));
    }

    private void handleRemove(CommandSourceStack stack, String[] args) {
        if (args.length < 2) {
            stack.getSender().sendMessage(Component.text("使用法: /questportal remove <id>", NamedTextColor.RED));
            return;
        }
        String id = args[1];
        if (repository.get(id) == null) {
            stack.getSender().sendMessage(Component.text("ポータル '" + id + "' が見つかりません。", NamedTextColor.RED));
            return;
        }
        repository.remove(id);
        stack.getSender().sendMessage(Component.text("ポータル '" + id + "' を削除しました。", NamedTextColor.GREEN));
    }

    private void handleList(CommandSourceStack stack) {
        var all = repository.getAll();
        if (all.isEmpty()) {
            stack.getSender().sendMessage(Component.text("登録されているポータルはありません。", NamedTextColor.GRAY));
            return;
        }
        stack.getSender().sendMessage(Component.text("=== クエストポータル一覧 ===", NamedTextColor.GOLD));
        for (PortalLocation p : all) {
            stack.getSender().sendMessage(Component.text(
                    p.id() + " → " + p.dungeonId() + " @ " + p.world() + " (" + (int) p.x() + ", " + (int) p.z() + ")",
                    NamedTextColor.AQUA));
        }
    }

    private void handleTp(CommandSourceStack stack, String[] args) {
        if (args.length < 2) {
            stack.getSender().sendMessage(Component.text("使用法: /questportal tp <id>", NamedTextColor.RED));
            return;
        }
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return;
        }
        PortalLocation p = repository.get(args[1]);
        if (p == null) {
            stack.getSender().sendMessage(Component.text("ポータル '" + args[1] + "' が見つかりません。", NamedTextColor.RED));
            return;
        }
        org.bukkit.World world = player.getServer().getWorld(p.world());
        if (world == null) {
            stack.getSender().sendMessage(Component.text("ワールドが見つかりません: " + p.world(), NamedTextColor.RED));
            return;
        }
        player.teleport(new org.bukkit.Location(world, p.x(), p.y(), p.z()));
        stack.getSender().sendMessage(Component.text("ポータル '" + p.id() + "' にテレポートしました。", NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSourceStack stack) {
        stack.getSender().sendMessage(Component.text("=== クエストポータルコマンド ===", NamedTextColor.GOLD));
        stack.getSender().sendMessage(Component.text("/questportal add <id> <dungeonId> - 現在地に追加", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/questportal remove <id> - 削除", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/questportal list - 一覧表示", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/questportal tp <id> - テレポート確認", NamedTextColor.YELLOW));
    }
}
