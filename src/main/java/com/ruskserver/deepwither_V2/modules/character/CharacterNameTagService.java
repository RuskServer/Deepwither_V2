package com.ruskserver.deepwither_V2.modules.character;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Optional;

@Service
public class CharacterNameTagService {
    private static final String SOFT_HARDCORE_TEAM = "dw_char_shc";
    private static final String TRUE_HARDCORE_TEAM = "dw_char_thc";

    private final CharacterService characterService;

    @Inject
    public CharacterNameTagService(CharacterService characterService) {
        this.characterService = characterService;
    }

    public void refresh(Player player) {
        Optional<GameCharacter> active = characterService.getActiveCharacter(player.getUniqueId());
        CharacterMode mode = active.map(GameCharacter::mode).orElse(CharacterMode.STANDARD);
        apply(player, mode);
    }

    public void refresh(Player player, CharacterMode mode) {
        apply(player, mode);
    }

    public void clear(Player player) {
        apply(player, CharacterMode.STANDARD);
    }

    private void apply(Player player, CharacterMode mode) {
        Component prefix = prefixComponent(mode);
        String entry = player.getName();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            applyToScoreboard(viewer.getScoreboard(), entry, mode);
        }
        applyToScoreboard(Bukkit.getScoreboardManager().getMainScoreboard(), entry, mode);

        Component visibleName = prefix.append(Component.text(player.getName(), NamedTextColor.WHITE));
        player.displayName(visibleName);
        player.playerListName(visibleName);
    }

    private void applyToScoreboard(Scoreboard scoreboard, String entry, CharacterMode mode) {
        Team softTeam = ensureTeam(scoreboard, SOFT_HARDCORE_TEAM, CharacterMode.SOFT_HARDCORE);
        Team trueTeam = ensureTeam(scoreboard, TRUE_HARDCORE_TEAM, CharacterMode.TRUE_HARDCORE);
        softTeam.removeEntry(entry);
        trueTeam.removeEntry(entry);

        if (mode == CharacterMode.SOFT_HARDCORE) {
            softTeam.addEntry(entry);
        } else if (mode == CharacterMode.TRUE_HARDCORE) {
            trueTeam.addEntry(entry);
        }
    }

    private Team ensureTeam(Scoreboard scoreboard, String teamName, CharacterMode mode) {
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.prefix(prefixComponent(mode));
        return team;
    }

    private Component prefixComponent(CharacterMode mode) {
        return switch (mode) {
            case STANDARD -> Component.empty();
            case SOFT_HARDCORE -> Component.text(mode.getNamePrefix(), NamedTextColor.GOLD);
            case TRUE_HARDCORE -> Component.text(mode.getNamePrefix(), NamedTextColor.RED);
        };
    }
}
