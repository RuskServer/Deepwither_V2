package com.ruskserver.deepwither_V2.modules.player.commands;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerAttributeProvider;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * プレイヤーの現在の基礎能力値と最終ステータスを表示するコマンド。
 */
@com.ruskserver.deepwither_V2.core.di.annotations.Component
@Command(name = "status", description = "現在のステータス詳細を表示します", aliases = {"st"})
public class CommandStatus implements BasicCommand {

    private static final String BORDER = "──────────────────────────────";

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
            stack.getSender().sendMessage("プレイヤーのみ実行できます。");
            return;
        }

        UUID uuid = player.getUniqueId();

        player.sendMessage(border());
        player.sendMessage(header(player));
        player.sendMessage(net.kyori.adventure.text.Component.empty());

        repository.get(uuid).ifPresent(data -> {
            PlayerAttributeProvider.AttributeData attrData = data.get(PlayerAttributeProvider.KEY);
            if (attrData != null) {
                player.sendMessage(sectionTitle("基本能力値"));
                for (AttributeType type : AttributeType.values()) {
                    player.sendMessage(statLine(type.getDisplayName(), attrData.getAttribute(type), NamedTextColor.GREEN));
                }
                player.sendMessage(statLine("残りポイント", attrData.getRemainingPoints(), NamedTextColor.AQUA));
                player.sendMessage(net.kyori.adventure.text.Component.empty());
            }
        });

        player.sendMessage(sectionTitle("最終ステータス"));
        displayStat(player, StatType.HEALTH, NamedTextColor.RED);
        displayStat(player, StatType.MAX_MANA, NamedTextColor.AQUA);
        displayStat(player, StatType.ATTACK_DAMAGE, NamedTextColor.GOLD);
        displayStat(player, StatType.DEFENSE, NamedTextColor.BLUE);
        displayStat(player, StatType.MAGIC_DAMAGE, NamedTextColor.LIGHT_PURPLE);
        displayStat(player, StatType.MAGIC_DEFENSE, NamedTextColor.DARK_PURPLE);
        player.sendMessage(statLine(StatType.CRITICAL_CHANCE.getDisplayName(), formatPercent(statManager.getTotalStat(player, StatType.CRITICAL_CHANCE)), NamedTextColor.GREEN));
        player.sendMessage(statLine(StatType.CRITICAL_DAMAGE.getDisplayName(), formatPercent(statManager.getTotalStat(player, StatType.CRITICAL_DAMAGE)), NamedTextColor.GREEN));

        player.sendMessage(border());
    }

    private void displayStat(Player player, StatType type, NamedTextColor color) {
        player.sendMessage(statLine(type.getDisplayName(), String.format("%,.1f", statManager.getTotalStat(player, type)), color));
    }

    private net.kyori.adventure.text.Component border() {
        return net.kyori.adventure.text.Component.text(BORDER, NamedTextColor.AQUA).decorate(TextDecoration.STRIKETHROUGH);
    }

    private net.kyori.adventure.text.Component header(Player player) {
        return net.kyori.adventure.text.Component.text("✦ ", NamedTextColor.WHITE)
                .append(net.kyori.adventure.text.Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(net.kyori.adventure.text.Component.text(" のステータス", NamedTextColor.WHITE));
    }

    private net.kyori.adventure.text.Component sectionTitle(String title) {
        return net.kyori.adventure.text.Component.text("[ ", NamedTextColor.DARK_GRAY)
                .append(net.kyori.adventure.text.Component.text(title, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(net.kyori.adventure.text.Component.text(" ]", NamedTextColor.DARK_GRAY));
    }

    private net.kyori.adventure.text.Component statLine(String label, Object value, NamedTextColor valueColor) {
        return net.kyori.adventure.text.Component.text("  - ", NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(label + ": ", NamedTextColor.WHITE))
                .append(net.kyori.adventure.text.Component.text(String.valueOf(value), valueColor));
    }

    private String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }
}
