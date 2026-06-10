package com.ruskserver.deepwither_V2.modules.admin.commands;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.provider.CharacterAttributeProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.provider.CharacterSkillTreeProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Command(name = "deepwither", description = "Deepwitherデバッグ・管理者コマンド", aliases = {"dw"})
public class CommandDeepwither implements BasicCommand {

    private static final Set<String> SUBCOMMANDS = Set.of(
            "skilltreepoints", "sp",
            "statpoints", "stp",
            "resetskilltree", "rst",
            "resetstats", "rs"
    );

    private final CharacterService characterService;
    private final CharacterDataRepository characterDataRepository;
    private final SkillTreeService skillTreeService;
    private final PlayerManager playerManager;

    @Inject
    public CommandDeepwither(CharacterService characterService, CharacterDataRepository characterDataRepository, SkillTreeService skillTreeService, PlayerManager playerManager) {
        this.characterService = characterService;
        this.characterDataRepository = characterDataRepository;
        this.skillTreeService = skillTreeService;
        this.playerManager = playerManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!stack.getSender().hasPermission("deepwither.admin")) {
            stack.getSender().sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sendUsage(stack);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "skilltreepoints", "sp" -> handleSkillTreePoints(stack, args);
            case "statpoints", "stp" -> handleStatPoints(stack, args);
            case "resetskilltree", "rst" -> handleResetSkillTree(stack, args);
            case "resetstats", "rs" -> handleResetStats(stack, args);
            default -> sendUsage(stack);
        }
    }

    private void handleSkillTreePoints(CommandSourceStack stack, String[] args) {
        if (args.length < 3) {
            stack.getSender().sendMessage(Component.text("使用法: /deepwither skilltreepoints <player> <amount>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            stack.getSender().sendMessage(Component.text("プレイヤーが見つかりません。", NamedTextColor.RED));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            stack.getSender().sendMessage(Component.text("数値を指定してください。", NamedTextColor.RED));
            return;
        }

        skillTreeService.addSkillPoints(target, amount, "admin");
        stack.getSender().sendMessage(Component.text(target.getName() + " にスキルポイントを " + amount + " 付与しました。", NamedTextColor.GREEN));
    }

    private void handleStatPoints(CommandSourceStack stack, String[] args) {
        if (args.length < 3) {
            stack.getSender().sendMessage(Component.text("使用法: /deepwither statpoints <player> <amount>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            stack.getSender().sendMessage(Component.text("プレイヤーが見つかりません。", NamedTextColor.RED));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            stack.getSender().sendMessage(Component.text("数値を指定してください。", NamedTextColor.RED));
            return;
        }

        characterService.getActiveCharacter(target.getUniqueId()).ifPresentOrElse(c ->
                characterDataRepository.get(c.characterId()).ifPresentOrElse(data -> {
                    CharacterAttributeProvider.AttributeData attrData = data.get(CharacterAttributeProvider.KEY);
                    if (attrData == null) {
                        stack.getSender().sendMessage(Component.text("属性データが見つかりません。", NamedTextColor.RED));
                        return;
                    }
                    attrData.addRemainingPoints(amount);
                    data.markDirty(CharacterAttributeProvider.KEY);
                    characterDataRepository.save(c.characterId(), data);
                    stack.getSender().sendMessage(Component.text(target.getName() + " に属性ポイントを " + amount + " 付与しました。", NamedTextColor.GREEN));
                }, () -> stack.getSender().sendMessage(Component.text("キャラクターデータが見つかりません。", NamedTextColor.RED))
        ), () -> stack.getSender().sendMessage(Component.text("アクティブなキャラクターがいません。", NamedTextColor.RED)));
    }

    private void handleResetSkillTree(CommandSourceStack stack, String[] args) {
        if (args.length < 2) {
            stack.getSender().sendMessage(Component.text("使用法: /deepwither resetskilltree <player>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            stack.getSender().sendMessage(Component.text("プレイヤーが見つかりません。", NamedTextColor.RED));
            return;
        }

        characterService.getActiveCharacter(target.getUniqueId()).ifPresentOrElse(c ->
                characterDataRepository.get(c.characterId()).ifPresentOrElse(data -> {
                    CharacterSkillTreeProvider.SkillTreeData treeData = data.get(CharacterSkillTreeProvider.KEY);
                    if (treeData == null) {
                        stack.getSender().sendMessage(Component.text("スキルツリーデータが見つかりません。", NamedTextColor.RED));
                        return;
                    }

                    Set<String> nodeIds = Set.copyOf(treeData.getUnlockedNodes().keySet());
                    for (String nodeId : nodeIds) {
                        treeData.setNodeLevel(nodeId, 0);
                    }
                    treeData.setSkillPoints(0);

                    data.markDirty(CharacterSkillTreeProvider.KEY);
                    characterDataRepository.save(c.characterId(), data);
                    skillTreeService.recalculatePassives(target);

                    stack.getSender().sendMessage(Component.text(target.getName() + " のスキルツリーをリセットしました。", NamedTextColor.GREEN));
                    target.sendMessage(Component.text("スキルツリーがリセットされました。", NamedTextColor.YELLOW));
                }, () -> stack.getSender().sendMessage(Component.text("キャラクターデータが見つかりません。", NamedTextColor.RED))
        ), () -> stack.getSender().sendMessage(Component.text("アクティブなキャラクターがいません。", NamedTextColor.RED)));
    }

    private void handleResetStats(CommandSourceStack stack, String[] args) {
        if (args.length < 2) {
            stack.getSender().sendMessage(Component.text("使用法: /deepwither resetstats <player>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            stack.getSender().sendMessage(Component.text("プレイヤーが見つかりません。", NamedTextColor.RED));
            return;
        }

        characterService.getActiveCharacter(target.getUniqueId()).ifPresentOrElse(c ->
                characterDataRepository.get(c.characterId()).ifPresentOrElse(data -> {
                    CharacterAttributeProvider.AttributeData attrData = data.get(CharacterAttributeProvider.KEY);
                    if (attrData == null) {
                        stack.getSender().sendMessage(Component.text("属性データが見つかりません。", NamedTextColor.RED));
                        return;
                    }

                    for (AttributeType type : AttributeType.values()) {
                        attrData.setAttribute(type, 0);
                    }
                    attrData.setRemainingPoints(0);

                    data.markDirty(CharacterAttributeProvider.KEY);
                    characterDataRepository.save(c.characterId(), data);
                    playerManager.recalculateStats(target);

                    stack.getSender().sendMessage(Component.text(target.getName() + " のステータスをリセットしました。", NamedTextColor.GREEN));
                    target.sendMessage(Component.text("ステータスがリセットされました。", NamedTextColor.YELLOW));
                }, () -> stack.getSender().sendMessage(Component.text("キャラクターデータが見つかりません。", NamedTextColor.RED))
        ), () -> stack.getSender().sendMessage(Component.text("アクティブなキャラクターがいません。", NamedTextColor.RED)));
    }

    private void sendUsage(CommandSourceStack stack) {
        stack.getSender().sendMessage(Component.text("--- /deepwither 使用方法 ---", NamedTextColor.GOLD));
        stack.getSender().sendMessage(Component.text("/dw skilltreepoints <player> <amount>  - スキルポイント付与", NamedTextColor.AQUA));
        stack.getSender().sendMessage(Component.text("/dw statpoints <player> <amount>      - 属性ポイント付与", NamedTextColor.AQUA));
        stack.getSender().sendMessage(Component.text("/dw resetskilltree <player>           - スキルツリーリセット", NamedTextColor.AQUA));
        stack.getSender().sendMessage(Component.text("/dw resetstats <player>               - ステータスリセット", NamedTextColor.AQUA));
        stack.getSender().sendMessage(Component.text("/dw sp / stp / rst / rs               - エイリアス", NamedTextColor.GRAY));
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .map(s -> switch (s) {
                        case "sp" -> "skilltreepoints";
                        case "stp" -> "statpoints";
                        case "rst" -> "resetskilltree";
                        case "rs" -> "resetstats";
                        default -> s;
                    })
                    .toList();
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
