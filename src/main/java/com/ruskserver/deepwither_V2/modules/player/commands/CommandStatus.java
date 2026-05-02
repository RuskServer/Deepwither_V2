package com.ruskserver.deepwither_V2.modules.player.commands;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerAttributeProvider;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * プレイヤーの現在の属性と詳細ステータスを表示するコマンド。
 */
@Component
@Command(name = "status", description = "現在のステータス詳細を表示します", aliases = {"st"})
public class CommandStatus implements BasicCommand {

    private final PlayerDataRepository repository;
    private final StatManager statManager;

    @Inject
    public CommandStatus(PlayerDataRepository repository, StatManager statManager) {
        this.repository = repository;
        this.statManager = statManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("プレイヤーのみ実行可能です。");
            return;
        }

        UUID uuid = player.getUniqueId();
        
        // ヘッダー
        player.sendMessage(Component.text("--------------------------------------", NamedTextColor.AQUA)
                .decorate(TextDecoration.STRIKETHROUGH));
        player.sendMessage(Component.text("       » ", NamedTextColor.WHITE)
                .append(Component.text(player.getName() + " のステータス", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" «", NamedTextColor.WHITE)));
        player.sendMessage(Component.empty());

        // 基本属性 (STR, VIT, etc)
        repository.get(uuid).ifPresent(data -> {
            PlayerAttributeProvider.AttributeData attrData = data.get(PlayerAttributeProvider.KEY);
            if (attrData != null) {
                player.sendMessage(Component.text("[ 基本属性 ]", NamedTextColor.YELLOW, TextDecoration.BOLD));
                for (AttributeType type : AttributeType.values()) {
                    player.sendMessage(Component.text("  - ", NamedTextColor.GRAY)
                            .append(Component.text(type.getDisplayName() + ": ", NamedTextColor.WHITE))
                            .append(Component.text(attrData.getAttribute(type), NamedTextColor.GREEN)));
                }
                player.sendMessage(Component.text("  - ", NamedTextColor.GRAY)
                        .append(Component.text("残りポイント: ", NamedTextColor.WHITE))
                        .append(Component.text(attrData.getRemainingPoints(), NamedTextColor.AQUA)));
                player.sendMessage(Component.empty());
            }
        });

        // 戦闘ステータス (最終値)
        player.sendMessage(Component.text("[ 戦闘ステータス ]", NamedTextColor.YELLOW, TextDecoration.BOLD));
        
        displayStat(player, StatType.HEALTH, NamedTextColor.RED);
        displayStat(player, StatType.MAX_MANA, NamedTextColor.AQUA);
        displayStat(player, StatType.ATTACK_DAMAGE, NamedTextColor.GOLD);
        displayStat(player, StatType.DEFENSE, NamedTextColor.BLUE);
        displayStat(player, StatType.MAGIC_DAMAGE, NamedTextColor.LIGHT_PURPLE);
        displayStat(player, StatType.MAGIC_DEFENSE, NamedTextColor.DARK_PURPLE);
        
        // クリティカル系などはパーセント表示が好ましい
        double critChance = statManager.getTotalStat(player, StatType.CRITICAL_CHANCE);
        double critDamage = statManager.getTotalStat(player, StatType.CRITICAL_DAMAGE);
        
        player.sendMessage(Component.text("  - ", NamedTextColor.GRAY)
                .append(Component.text(StatType.CRITICAL_CHANCE.getDisplayName() + ": ", NamedTextColor.WHITE))
                .append(Component.text(String.format("%.1f%%", critChance * 100), NamedTextColor.GREEN)));
        
        player.sendMessage(Component.text("  - ", NamedTextColor.GRAY)
                .append(Component.text(StatType.CRITICAL_DAMAGE.getDisplayName() + ": ", NamedTextColor.WHITE))
                .append(Component.text(String.format("%.1f%%", critDamage * 100), NamedTextColor.GREEN)));

        player.sendMessage(Component.text("--------------------------------------", NamedTextColor.AQUA)
                .decorate(TextDecoration.STRIKETHROUGH));
    }

    private void displayStat(Player player, StatType type, NamedTextColor color) {
        double value = statManager.getTotalStat(player, type);
        player.sendMessage(Component.text("  - ", NamedTextColor.GRAY)
                .append(Component.text(type.getDisplayName() + ": ", NamedTextColor.WHITE))
                .append(Component.text(String.format("%,.1f", value), color)));
    }
}
