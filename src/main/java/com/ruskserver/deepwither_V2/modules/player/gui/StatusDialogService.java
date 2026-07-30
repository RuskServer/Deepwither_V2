package com.ruskserver.deepwither_V2.modules.player.gui;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.AttributeType;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.provider.CharacterAttributeProvider;
import com.ruskserver.deepwither_V2.modules.player.provider.CharacterLevelProvider;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderReputationService;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StatusDialogService {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##");
    private static final int BODY_WIDTH = 360;
    private static final double BASE_CRITICAL_MULTIPLIER_PERCENT = 150.0;

    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final StatManager statManager;
    private final VirtualHealthManager healthManager;
    private final PlayerManager playerManager;
    private final TraderService traderService;
    private final TraderReputationService reputationService;

    @Inject
    public StatusDialogService(
            CharacterDataRepository characterDataRepository,
            CharacterService characterService,
            StatManager statManager,
            VirtualHealthManager healthManager,
            PlayerManager playerManager,
            TraderService traderService,
            TraderReputationService reputationService) {
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.statManager = statManager;
        this.healthManager = healthManager;
        this.playerManager = playerManager;
        this.traderService = traderService;
        this.reputationService = reputationService;
    }

    public void open(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(buildBasicInfo(player), BODY_WIDTH));
        bodies.add(DialogBody.plainMessage(buildAttributes(player), BODY_WIDTH));
        bodies.add(DialogBody.plainMessage(buildCombatStats(player), BODY_WIDTH));
        bodies.add(DialogBody.plainMessage(buildTraderReputation(player), BODY_WIDTH));

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("プレイヤー ステータス", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, true))
                        .externalTitle(Component.text("ステータス", NamedTextColor.YELLOW))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(bodies)
                        .build())
                .type(DialogType.notice()));

        player.showDialog(dialog);
    }

    private Component buildBasicInfo(Player player) {
        Component content = sectionTitle("基本情報")
                .append(Component.newline())
                .append(line("プレイヤー", player.getName(), NamedTextColor.WHITE));

        var activeCharacter = characterService.getActiveCharacter(player.getUniqueId());
        if (activeCharacter.isPresent()) {
            var character = activeCharacter.get();
            content = content.append(Component.newline())
                    .append(line("キャラクター", character.name(), NamedTextColor.GREEN));

            var data = characterDataRepository.get(character.characterId());
            if (data.isPresent()) {
                CharacterLevelProvider.LevelData levelData = data.get().get(CharacterLevelProvider.KEY);
                if (levelData != null) {
                    int level = levelData.getLevel();
                    int exp = levelData.getExp();
                    int nextExp = playerManager.getExpToNextLevel(level);
                    double percent = nextExp > 0 && nextExp != Integer.MAX_VALUE
                            ? (double) exp / nextExp * 100.0
                            : 0.0;
                    content = content.append(Component.newline())
                            .append(line(
                                    "レベル",
                                    String.format("%d  (%.1f%%)", level, percent),
                                    NamedTextColor.GREEN
                            ));
                }
            }
        }

        return content.append(Component.newline())
                .append(line(
                        "所持金",
                        formatMoney(traderService.getBalance(player)),
                        NamedTextColor.GOLD
                ));
    }

    private Component buildAttributes(Player player) {
        Component content = sectionTitle("基本能力値");
        var activeCharacter = characterService.getActiveCharacter(player.getUniqueId());
        if (activeCharacter.isEmpty()) {
            return content.append(Component.newline())
                    .append(Component.text("データなし", NamedTextColor.DARK_GRAY));
        }

        var data = characterDataRepository.get(activeCharacter.get().characterId());
        if (data.isEmpty()) {
            return content.append(Component.newline())
                    .append(Component.text("データなし", NamedTextColor.DARK_GRAY));
        }

        CharacterAttributeProvider.AttributeData attributeData =
                data.get().get(CharacterAttributeProvider.KEY);
        if (attributeData == null) {
            return content.append(Component.newline())
                    .append(Component.text("データなし", NamedTextColor.DARK_GRAY));
        }

        for (AttributeType type : AttributeType.values()) {
            content = content.append(Component.newline())
                    .append(line(
                            type.getDisplayName(),
                            String.valueOf(attributeData.getAttribute(type)),
                            NamedTextColor.GREEN
                    ));
        }
        return content.append(Component.newline())
                .append(line(
                        "残りポイント",
                        String.valueOf(attributeData.getRemainingPoints()),
                        NamedTextColor.AQUA
                ));
    }

    private Component buildCombatStats(Player player) {
        double currentHp = healthManager.getHealth(player);
        double maxHp = healthManager.getMaxHealth(player);
        double defense = statManager.getTotalStat(player, StatType.DEFENSE);
        double magicDefense = statManager.getTotalStat(player, StatType.MAGIC_DEFENSE);
        double criticalChancePercent =
                statManager.getTotalStat(player, StatType.CRITICAL_CHANCE);
        double criticalMultiplierPercent = BASE_CRITICAL_MULTIPLIER_PERCENT
                + statManager.getTotalStat(player, StatType.CRITICAL_DAMAGE);

        return sectionTitle("戦闘ステータス")
                .append(Component.newline())
                .append(line(
                        "HP",
                        String.format("%.0f / %.0f", currentHp, maxHp),
                        NamedTextColor.WHITE
                ))
                .append(Component.newline())
                .append(statLine(
                        "攻撃力",
                        statManager.getTotalStat(player, StatType.ATTACK_DAMAGE),
                        NamedTextColor.RED
                ))
                .append(Component.newline())
                .append(statLine(
                        "魔法攻撃力",
                        statManager.getTotalStat(player, StatType.MAGIC_DAMAGE),
                        NamedTextColor.LIGHT_PURPLE
                ))
                .append(Component.newline())
                .append(statLine(
                        "防御力",
                        defense,
                        NamedTextColor.BLUE,
                        buildEffectiveHpHover(maxHp, defense)
                ))
                .append(Component.newline())
                .append(statLine(
                        "魔法防御力",
                        magicDefense,
                        NamedTextColor.DARK_AQUA,
                        buildEffectiveHpHover(maxHp, magicDefense)
                ))
                .append(Component.newline())
                .append(statLine(
                        "クリティカル率",
                        criticalChancePercent,
                        NamedTextColor.GOLD,
                        "%"
                ))
                .append(Component.newline())
                .append(statLine(
                        "クリティカル倍率",
                        criticalMultiplierPercent,
                        NamedTextColor.GOLD,
                        "%"
                ))
                .append(Component.newline())
                .append(statLine(
                        "移動速度",
                        statManager.getTotalStat(player, StatType.SPEED),
                        NamedTextColor.AQUA,
                        "%"
                ));
    }

    private Component buildTraderReputation(Player player) {
        Component content = sectionTitle("トレーダー信用度");
        var traders = traderService.getAllTraders();
        if (traders.isEmpty()) {
            return content.append(Component.newline())
                    .append(Component.text("データなし", NamedTextColor.DARK_GRAY));
        }

        var traderIds = traders.keySet().stream()
                .sorted(Comparator.comparing(id -> traders.get(id).getDisplayName()))
                .toList();
        for (String traderId : traderIds) {
            content = content.append(Component.newline())
                    .append(line(
                            traders.get(traderId).getDisplayName(),
                            String.valueOf(reputationService.getReputation(player, traderId)),
                            NamedTextColor.AQUA
                    ));
        }
        return content;
    }

    private Component sectionTitle(String title) {
        return Component.text("◆ " + title, NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true);
    }

    private Component line(String label, String value, NamedTextColor valueColor) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, valueColor));
    }

    private Component statLine(
            String label,
            double value,
            NamedTextColor valueColor) {
        return line(label, String.format("%.1f", value), valueColor);
    }

    private Component statLine(
            String label,
            double value,
            NamedTextColor valueColor,
            Component hover) {
        Component component = line(label, String.format("%.1f", value), valueColor);
        return hover == null ? component : component.hoverEvent(HoverEvent.showText(hover));
    }

    private Component statLine(
            String label,
            double value,
            NamedTextColor valueColor,
            String suffix) {
        return line(label, String.format("%.1f%s", value, suffix), valueColor);
    }

    private Component buildEffectiveHpHover(double maxHp, double statValue) {
        double effectiveHp = maxHp * (1.0 + statValue / 250.0);
        return Component.text("実効HP (Effective HP)", NamedTextColor.YELLOW)
                .append(Component.newline())
                .append(Component.text("全体: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%,.0f", effectiveHp), NamedTextColor.GREEN));
    }

    private String formatMoney(double amount) {
        return "$" + MONEY_FORMAT.format(Math.max(0.0, amount));
    }
}
