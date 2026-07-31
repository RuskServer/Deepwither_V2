package com.ruskserver.deepwither_V2.modules.mining;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionService;
import com.ruskserver.deepwither_V2.modules.profession.ProfessionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class MiningSkillService {

    private final ProfessionService professionService;

    @Inject
    public MiningSkillService(ProfessionService professionService) {
        this.professionService = professionService;
    }

    public MiningProfile resolveProfile(Player player) {
        int level = professionService.getProgress(player, ProfessionType.MINING).level();
        return new MiningProfile(
                level,
                resolveBaseDamage(level),
                Math.min(0.30D, 0.08D + level * 0.0022D),
                Math.min(2, 1 + level / 60),
                Math.min(0.30D, 0.10D + level * 0.0020D),
                Math.min(0.18D, 0.02D + level * 0.0015D),
                Math.min(2, 1 + level / 60),
                Math.min(6, 2 + level / 25)
        );
    }

    public MiningStrike resolveStrike(MiningProfile profile) {
        boolean critical = ThreadLocalRandom.current().nextDouble() < profile.criticalChance();
        int damage = profile.baseDamage() + (critical ? profile.criticalBonus() : 0);
        return new MiningStrike(Math.max(1, damage), critical);
    }

    public double adjustDropChance(MiningProfile profile, double baseChance) {
        double chance = Math.max(0.0D, Math.min(1.0D, baseChance));
        if (chance >= 1.0D) {
            return 1.0D;
        }
        double boosted = chance + (1.0D - chance) * (1.0D - chance) * profile.rareDropLuck();
        return Math.max(chance, Math.min(1.0D, boosted));
    }

    public GeologicalBurst resolveGeologicalBurst(MiningProfile profile) {
        boolean triggered = ThreadLocalRandom.current().nextDouble() < profile.geologicalChance();
        return new GeologicalBurst(triggered, profile.geologicalRadius(), profile.geologicalLimit());
    }

    public Component buildStatus(ProfessionService.ProfessionProgress progress) {
        MiningProfile profile = new MiningProfile(
                progress.level(),
                resolveBaseDamage(progress.level()),
                Math.min(0.30D, 0.08D + progress.level() * 0.0022D),
                Math.min(2, 1 + progress.level() / 60),
                Math.min(0.30D, 0.10D + progress.level() * 0.0020D),
                Math.min(0.18D, 0.02D + progress.level() * 0.0015D),
                Math.min(2, 1 + progress.level() / 60),
                Math.min(6, 2 + progress.level() / 25)
        );

        String experience = progress.requiredExperience() <= 0L
                ? "MAX"
                : progress.currentExperience() + " / " + progress.requiredExperience();
        return Component.text("◆ 採掘職", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("レベル: ", NamedTextColor.GRAY))
                .append(Component.text(profile.level(), NamedTextColor.YELLOW))
                .append(Component.text("  EXP: ", NamedTextColor.GRAY))
                .append(Component.text(experience, NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("採掘威力: ", NamedTextColor.GRAY))
                .append(Component.text(profile.baseDamage(), NamedTextColor.RED))
                .append(Component.text("  クリティカル: ", NamedTextColor.GRAY))
                .append(Component.text(formatPercent(profile.criticalChance()), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("レア採掘補正: ", NamedTextColor.GRAY))
                .append(Component.text(formatPercent(profile.rareDropLuck()), NamedTextColor.AQUA))
                .append(Component.text("  地殻破壊: ", NamedTextColor.GRAY))
                .append(Component.text(formatPercent(profile.geologicalChance()), NamedTextColor.GREEN));
    }

    private int resolveBaseDamage(int level) {
        if (level >= 90) return 4;
        if (level >= 60) return 3;
        if (level >= 30) return 2;
        return 1;
    }

    private String formatPercent(double value) {
        return String.format("%.1f%%", value * 100.0D);
    }

    public record MiningProfile(
            int level,
            int baseDamage,
            double criticalChance,
            int criticalBonus,
            double rareDropLuck,
            double geologicalChance,
            int geologicalRadius,
            int geologicalLimit) {
    }

    public record MiningStrike(int damage, boolean critical) {
    }

    public record GeologicalBurst(boolean triggered, int radius, int maxBlocks) {
    }
}
