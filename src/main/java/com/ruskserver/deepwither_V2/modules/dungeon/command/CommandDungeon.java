package com.ruskserver.deepwither_V2.modules.dungeon.command;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinitionRegistry;
import com.ruskserver.deepwither_V2.modules.dungeon.instance.DungeonInstance;
import com.ruskserver.deepwither_V2.modules.dungeon.instance.DungeonInstanceManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * /dungeon コマンド。
 * <p>
 * ダンジョンの生成・参加・状態確認を行います。
 * <ul>
 *   <li>/dungeon start &lt;ダンジョンID&gt; - ダンジョンを生成して開始</li>
 *   <li>/dungeon join &lt;インスタンスID&gt; - ダンジョンに参加</li>
 *   <li>/dungeon leave - ダンジョンから離脱</li>
 *   <li>/dungeon list - ダンジョン一覧を表示</li>
 *   <li>/dungeon info - 現在のダンジョン情報を表示</li>
 * </ul>
 */
@Command(
        name = "dungeon",
        description = "ダンジョンコマンド",
        aliases = {"dg"}
)
public class CommandDungeon implements BasicCommand {

    private final DungeonDefinitionRegistry definitionRegistry;
    private final DungeonInstanceManager instanceManager;

    @Inject
    public CommandDungeon(DungeonDefinitionRegistry definitionRegistry, DungeonInstanceManager instanceManager) {
        this.definitionRegistry = definitionRegistry;
        this.instanceManager = instanceManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(stack);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(stack, args);
            case "join" -> handleJoin(stack, args);
            case "leave" -> handleLeave(stack);
            case "list" -> handleList(stack);
            case "info" -> handleInfo(stack);
            default -> sendHelp(stack);
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("start", "join", "leave", "list", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "start" -> definitionRegistry.getAll().stream()
                        .map(DungeonDefinition::id)
                        .filter(id -> id.startsWith(args[1].toLowerCase()))
                        .toList();
                case "join" -> instanceManager.getActiveInstances().stream()
                        .map(DungeonInstance::getInstanceId)
                        .filter(id -> id.startsWith(args[1].toLowerCase()))
                        .toList();
                default -> List.of();
            };
        }
        return List.of();
    }

    private void handleStart(CommandSourceStack stack, String[] args) {
        if (args.length < 2) {
            stack.getSender().sendMessage(Component.text("使用法: /dungeon start <ダンジョンID>", NamedTextColor.RED));
            return;
        }

        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("このコマンドはプレイヤーのみ使用できます。", NamedTextColor.RED));
            return;
        }

        String definitionId = args[1];
        DungeonDefinition definition = definitionRegistry.get(definitionId);
        if (definition == null) {
            stack.getSender().sendMessage(Component.text("ダンジョン定義 '" + definitionId + "' が見つかりません。", NamedTextColor.RED));
            return;
        }

        // 事前に参加状態をチェック
        DungeonInstance currentInstance = instanceManager.getPlayerInstance(player.getUniqueId());
        if (currentInstance != null) {
            stack.getSender().sendMessage(Component.text("既にダンジョンに参加しています: " + currentInstance.getInstanceId(), NamedTextColor.RED));
            return;
        }

        stack.getSender().sendMessage(Component.text("ダンジョン生成中: " + definition.displayName() + "...", NamedTextColor.YELLOW));

        // void ワールドを作成
        WorldCreator creator = new WorldCreator("dungeon_" + definitionId + "_" + System.currentTimeMillis())
                .environment(org.bukkit.World.Environment.NORMAL)
                .generator(new com.ruskserver.deepwither_V2.modules.dungeon.generator.VoidChunkGenerator());

        DungeonInstance instance = instanceManager.createInstance(definitionId, creator);
        if (instance == null) {
            stack.getSender().sendMessage(Component.text("ダンジョン生成に失敗しました。", NamedTextColor.RED));
            return;
        }

        if (!instanceManager.joinDungeon(player.getUniqueId(), instance.getInstanceId())) {
            stack.getSender().sendMessage(Component.text("ダンジョン参加処理に失敗しました。", NamedTextColor.RED));
            return;
        }

        stack.getSender().sendMessage(Component.text("ダンジョン生成完了: " + definition.displayName(), NamedTextColor.GREEN));
        stack.getSender().sendMessage(Component.text("インスタンスID: " + instance.getInstanceId(), NamedTextColor.AQUA));
        stack.getSender().sendMessage(Component.text("残りライフ: " + instance.getRemainingLives() + "/" + instance.getMaxLives(), NamedTextColor.GOLD));
        stack.getSender().sendMessage(Component.text("制限時間: " + definition.timeLimitMinutes() + "分", NamedTextColor.GOLD));
    }

    private void handleJoin(CommandSourceStack stack, String[] args) {
        if (args.length < 2) {
            stack.getSender().sendMessage(Component.text("使用法: /dungeon join <インスタンスID>", NamedTextColor.RED));
            return;
        }

        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("このコマンドはプレイヤーのみ使用できます。", NamedTextColor.RED));
            return;
        }

        String instanceId = args[1];

        DungeonInstance currentInstance = instanceManager.getPlayerInstance(player.getUniqueId());
        if (currentInstance != null) {
            stack.getSender().sendMessage(Component.text("既に別のダンジョンに参加しています: " + currentInstance.getInstanceId(), NamedTextColor.RED));
            return;
        }

        if (instanceManager.joinDungeon(player.getUniqueId(), instanceId)) {
            stack.getSender().sendMessage(Component.text("ダンジョンに参加しました: " + instanceId, NamedTextColor.GREEN));
        } else {
            stack.getSender().sendMessage(Component.text("ダンジョン '" + instanceId + "' に参加できませんでした。", NamedTextColor.RED));
        }
    }

    private void handleLeave(CommandSourceStack stack) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("このコマンドはプレイヤーのみ使用できます。", NamedTextColor.RED));
            return;
        }

        DungeonInstance instance = instanceManager.getPlayerInstance(player.getUniqueId());
        if (instance == null) {
            stack.getSender().sendMessage(Component.text("ダンジョンに参加していません。", NamedTextColor.RED));
            return;
        }

        instanceManager.leaveDungeon(player.getUniqueId());
        stack.getSender().sendMessage(Component.text("ダンジョンから離脱しました。", NamedTextColor.YELLOW));
    }

    private void handleList(CommandSourceStack stack) {
        var instances = instanceManager.getActiveInstances();
        if (instances.isEmpty()) {
            stack.getSender().sendMessage(Component.text("アクティブなダンジョンはありません。", NamedTextColor.GRAY));
            return;
        }

        stack.getSender().sendMessage(Component.text("=== アクティブダンジョン ===", NamedTextColor.GOLD));
        for (DungeonInstance instance : instances) {
            stack.getSender().sendMessage(Component.text(
                    instance.getInstanceId() + " - " + instance.getDefinition().displayName()
                            + " [" + instance.getState() + "] 参加者: " + instance.getParticipants().size(),
                    NamedTextColor.AQUA
            ));
        }
    }

    private void handleInfo(CommandSourceStack stack) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage(Component.text("このコマンドはプレイヤーのみ使用できます。", NamedTextColor.RED));
            return;
        }

        DungeonInstance instance = instanceManager.getPlayerInstance(player.getUniqueId());
        if (instance == null) {
            stack.getSender().sendMessage(Component.text("ダンジョンに参加していません。", NamedTextColor.RED));
            return;
        }

        stack.getSender().sendMessage(Component.text("=== ダンジョン情報 ===", NamedTextColor.GOLD));
        stack.getSender().sendMessage(Component.text("ID: " + instance.getInstanceId(), NamedTextColor.AQUA));
        stack.getSender().sendMessage(Component.text("名前: " + instance.getDefinition().displayName(), NamedTextColor.AQUA));
        stack.getSender().sendMessage(Component.text("状態: " + instance.getState(), NamedTextColor.AQUA));
        stack.getSender().sendMessage(Component.text("残りライフ: " + instance.getRemainingLives() + "/" + instance.getMaxLives(), NamedTextColor.GOLD));
        stack.getSender().sendMessage(Component.text("制限時間: " + instance.getDefinition().timeLimitMinutes() + "分", NamedTextColor.GOLD));
        stack.getSender().sendMessage(Component.text("配置ルーム数: " + instance.getLayout().getPlacedRoomCount(), NamedTextColor.AQUA));
    }

    private void sendHelp(CommandSourceStack stack) {
        stack.getSender().sendMessage(Component.text("=== ダンジョンコマンド ===", NamedTextColor.GOLD));
        stack.getSender().sendMessage(Component.text("/dungeon start <ID> - ダンジョンを開始", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/dungeon join <ID> - ダンジョンに参加", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/dungeon leave - ダンジョンから離脱", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/dungeon list - 一覧表示", NamedTextColor.YELLOW));
        stack.getSender().sendMessage(Component.text("/dungeon info - 情報表示", NamedTextColor.YELLOW));
    }
}
