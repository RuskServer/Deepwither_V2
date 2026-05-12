package com.ruskserver.deepwither_V2.modules.player.commands;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerAttributeProvider;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerLevelProvider;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderReputationService;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
@Command(name = "status", description = "現在のステータス詳細を表示します", aliases = {})
public class CommandStatus implements BasicCommand {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##");

    private final PlayerDataRepository repository;
    private final StatManager statManager;
    private final PlayerManager playerManager;
    private final TraderService traderService;
    private final TraderReputationService reputationService;

    @Inject
    public CommandStatus(PlayerDataRepository repository, StatManager statManager,
                         PlayerManager playerManager, TraderService traderService,
                         TraderReputationService reputationService) {
        this.repository = repository;
        this.statManager = statManager;
        this.playerManager = playerManager;
        this.traderService = traderService;
        this.reputationService = reputationService;
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
        player.sendMessage(Component.empty());

        player.sendMessage(sectionTitle("基本情報"));
        repository.get(uuid).ifPresent(data -> {
            PlayerLevelProvider.LevelData levelData = data.get(PlayerLevelProvider.KEY);
            if (levelData != null) {
                int level = levelData.getLevel();
                int exp = levelData.getExp();
                int nextExp = playerManager.getExpToNextLevel(level);
                double percent = nextExp > 0 && nextExp != Integer.MAX_VALUE
                        ? (double) exp / nextExp * 100 : 0.0;

                player.sendMessage(Component.text("  Lv: ", NamedTextColor.GRAY)
                        .append(Component.text(level, NamedTextColor.GREEN))
                        .append(Component.text(String.format(" (%.1f%%)", percent), NamedTextColor.YELLOW)));
            }
        });
        player.sendMessage(Component.text("  所持金: ", NamedTextColor.GRAY)
                .append(Component.text(formatMoney(traderService.getBalance(player)), NamedTextColor.GOLD)));
        player.sendMessage(Component.empty());

        player.sendMessage(sectionTitle("基本能力値"));
        repository.get(uuid).ifPresent(data -> {
            PlayerAttributeProvider.AttributeData attrData = data.get(PlayerAttributeProvider.KEY);
            if (attrData != null) {
                for (AttributeType type : AttributeType.values()) {
                    player.sendMessage(statLine(type.getDisplayName(), attrData.getAttribute(type), NamedTextColor.GREEN));
                }
                player.sendMessage(statLine("残りポイント", attrData.getRemainingPoints(), NamedTextColor.AQUA));
            }
        });
        player.sendMessage(Component.empty());

        player.sendMessage(sectionTitle("戦闘ステータス"));
        double currentHp = player.getHealth();
        double maxHp = statManager.getTotalStat(player, StatType.HEALTH);

        player.sendMessage(Component.text("  HP: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.0f", currentHp), NamedTextColor.WHITE))
                .append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.0f", maxHp), NamedTextColor.WHITE)));

        player.sendMessage(Component.text("  ")
                .append(statValue("攻撃力", statManager.getTotalStat(player, StatType.ATTACK_DAMAGE), NamedTextColor.RED, null))
                .append(Component.text("   "))
                .append(statValue("防御力", statManager.getTotalStat(player, StatType.DEFENSE), NamedTextColor.BLUE,
                        buildEffectiveHpHover(maxHp, statManager.getTotalStat(player, StatType.DEFENSE)))));

        player.sendMessage(Component.text("  ")
                .append(statValue("魔法攻撃力", statManager.getTotalStat(player, StatType.MAGIC_DAMAGE), NamedTextColor.LIGHT_PURPLE, null))
                .append(Component.text("   "))
                .append(statValue("魔法防御力", statManager.getTotalStat(player, StatType.MAGIC_DEFENSE), NamedTextColor.DARK_AQUA,
                        buildEffectiveHpHover(maxHp, statManager.getTotalStat(player, StatType.MAGIC_DEFENSE)))));

        player.sendMessage(Component.text("  ")
                .append(statValue("クリ率", statManager.getTotalStat(player, StatType.CRITICAL_CHANCE) * 100, NamedTextColor.GOLD, null))
                .append(Component.text("   "))
                .append(statValue("クリ倍率", statManager.getTotalStat(player, StatType.CRITICAL_DAMAGE) * 100, NamedTextColor.GOLD, null)));
        player.sendMessage(Component.empty());

        player.sendMessage(sectionTitle("信用度"));
        var allTraders = traderService.getAllTraders();
        if (allTraders.isEmpty()) {
            player.sendMessage(Component.text("  (データなし)", NamedTextColor.GRAY));
        } else {
            var ids = allTraders.keySet().stream().toList();
            for (int i = 0; i < ids.size(); i += 2) {
                String id1 = ids.get(i);
                String name1 = allTraders.get(id1).getDisplayName();
                int rep1 = reputationService.getReputation(player, id1);
                Component entry1 = Component.text("  " + name1 + ": ", NamedTextColor.GRAY)
                        .append(Component.text(rep1, NamedTextColor.AQUA));

                if (i + 1 < ids.size()) {
                    String id2 = ids.get(i + 1);
                    String name2 = allTraders.get(id2).getDisplayName();
                    int rep2 = reputationService.getReputation(player, id2);
                    Component entry2 = Component.text("  " + name2 + ": ", NamedTextColor.GRAY)
                            .append(Component.text(rep2, NamedTextColor.AQUA));
                    player.sendMessage(entry1.append(Component.text("    ")).append(entry2));
                } else {
                    player.sendMessage(entry1);
                }
            }
        }

        player.sendMessage(border());
    }

    private Component border() {
        return Component.text("---------------------------------------", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.STRIKETHROUGH, true);
    }

    private Component header(Player player) {
        return Component.text("       【 プレイヤー ステータス 】", NamedTextColor.YELLOW);
    }

    private Component sectionTitle(String title) {
        return Component.text(" [ " + title + " ]", NamedTextColor.AQUA);
    }

    private Component statLine(String label, Object value, NamedTextColor valueColor) {
        return Component.text("  " + label + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(value), valueColor));
    }

    private Component statValue(String name, double value, NamedTextColor color, Component hover) {
        String valStr = String.format("%.1f", value);
        Component comp = Component.text(name + ": ", NamedTextColor.GRAY)
                .append(Component.text(valStr, color));
        if (hover != null) {
            comp = comp.hoverEvent(HoverEvent.showText(hover));
        }
        return comp;
    }

    private Component buildEffectiveHpHover(double maxHp, double statValue) {
        double ehp = maxHp * (1.0 + (statValue / 250.0));
        return Component.text("実効HP (Effective HP)", NamedTextColor.YELLOW)
                .append(Component.newline())
                .append(Component.text("  全体: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%,.0f", ehp), NamedTextColor.GREEN));
    }

    private String formatMoney(double amount) {
        return "$" + MONEY_FORMAT.format(Math.max(0.0, amount));
    }
}
