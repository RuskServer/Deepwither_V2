package com.ruskserver.deepwither_V2.modules.dungeon.command;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinitionRegistry;
import com.ruskserver.deepwither_V2.modules.dungeon.instance.DungeonInstance;
import com.ruskserver.deepwither_V2.modules.dungeon.instance.DungeonInstanceManager;
import com.ruskserver.deepwither_V2.modules.dungeon.room.RoomTemplateRegistry;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * /dungeonadmin コマンド。
 * <p>
 * ダンジョン管理用の管理者コマンド。
 * <ul>
 *   <li>/dungeonadmin list - 全ダンジョン定義を表示</li>
 *   <li>/dungeonadmin templates - 読み込み済みテンプレート数を表示</li>
 *   <li>/dungeonadmin forceend &lt;インスタンスID&gt; - ダンジョンを強制終了</li>
 * </ul>
 */
@Command(
        name = "dungeonadmin",
        description = "ダンジョン管理コマンド",
        aliases = {"dgadmin"}
)
public class CommandDungeonAdmin implements BasicCommand {

    private final DungeonDefinitionRegistry definitionRegistry;
    private final DungeonInstanceManager instanceManager;
    private final RoomTemplateRegistry templateRegistry;

    @Inject
    public CommandDungeonAdmin(
            DungeonDefinitionRegistry definitionRegistry,
            DungeonInstanceManager instanceManager,
            RoomTemplateRegistry templateRegistry
    ) {
        this.definitionRegistry = definitionRegistry;
        this.instanceManager = instanceManager;
        this.templateRegistry = templateRegistry;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!stack.getSender().hasPermission("deepwither.dungeon.admin")) {
            stack.getSender().sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            sendHelp(stack);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(stack);
            case "templates" -> handleTemplates(stack);
            case "forceend" -> handleForceEnd(stack, args);
            default -> sendHelp(stack);
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("list", "templates", "forceend").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("forceend")) {
            return instanceManager.getActiveInstances().stream()
                    .map(DungeonInstance::getInstanceId)
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    private void handleList(CommandSourceStack stack) {
        stack.getSender().sendMessage(Component.text("=== ダンジョン定義一覧 ===", NamedTextColor.GOLD));
        for (DungeonDefinition def : definitionRegistry.getAll()) {
            stack.getSender().sendMessage(Component.text(
                    def.id() + " - " + def.displayName()
                            + " [depth=" + def.maxDepth() + ", lives=" + def.lives()
                            + ", branch=" + def.branchChance() + "]",
                    NamedTextColor.AQUA
            ));
        }
        stack.getSender().sendMessage(Component.text("合計: " + definitionRegistry.getCount() + " 定義", NamedTextColor.GRAY));
    }

    private void handleTemplates(CommandSourceStack stack) {
        stack.getSender().sendMessage(Component.text(
                "読み込み済みテンプレート数: " + templateRegistry.getTemplateCount(),
                NamedTextColor.AQUA
        ));
    }

    private void handleForceEnd(CommandSourceStack stack, String[] args) {
        if (args.length < 2) {
            stack.getSender().sendMessage(Component.text("使用法: /dungeonadmin forceend <インスタンスID>", NamedTextColor.RED));
            return;
        }

        String instanceId = args[1];
        DungeonInstance instance = instanceManager.getInstance(instanceId);
        if (instance == null) {
            stack.getSender().sendMessage(Component.text("インスタンス '" + instanceId + "' が見つかりません。", NamedTextColor.RED));
            return;
        }

        instanceManager.endDungeon(instance);
        stack.getSender().sendMessage(Component.text("ダンジョンを強制終了しました: " + instanceId, NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSourceStack stack) {
        stack.getSender().sendMessage(Component.text("=== ダンジョン管理コマンド ===", NamedTextColor.GOLD));
        stack.getSender().sendMessage(Component.text("/dungeonadmin list - 定義一覧", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/dungeonadmin templates - テンプレート数", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/dungeonadmin forceend <ID> - 強制終了", NamedTextColor.YELLOW));
    }
}
