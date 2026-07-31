package com.ruskserver.deepwither_V2.modules.profession;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.profession.provider.CharacterProfessionProvider;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfessionService implements Listener, Stoppable {

    private static final int MAX_LEVEL = 100;
    private static final int BASE_EXP = 50;
    private static final long BOSS_BAR_DURATION_TICKS = 100L;

    private final JavaPlugin plugin;
    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final Map<UUID, BossBarState> bossBars = new HashMap<>();

    @Inject
    public ProfessionService(
            JavaPlugin plugin,
            CharacterDataRepository characterDataRepository,
            CharacterService characterService) {
        this.plugin = plugin;
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
    }

    public ProfessionProgress getProgress(Player player, ProfessionType type) {
        long totalExperience = getExperience(player, type);
        int level = getLevel(totalExperience);
        long levelStart = getTotalExperienceForLevel(level);
        long required = level >= MAX_LEVEL ? 0L : getExperienceForNextLevel(level);
        long current = level >= MAX_LEVEL ? 0L : Math.max(0L, totalExperience - levelStart);
        return new ProfessionProgress(type, level, totalExperience, current, required);
    }

    public long getExperience(Player player, ProfessionType type) {
        return characterService.getActiveCharacter(player.getUniqueId())
                .flatMap(character -> characterDataRepository.get(character.characterId()))
                .map(data -> data.get(CharacterProfessionProvider.KEY))
                .map(professions -> professions.getExperience(type))
                .orElse(0L);
    }

    public void addExperience(Player player, ProfessionType type, long amount) {
        if (amount <= 0L) {
            return;
        }

        characterService.getActiveCharacter(player.getUniqueId()).ifPresent(character ->
                characterDataRepository.get(character.characterId()).ifPresent(data -> {
                    ProfessionData professions = data.get(CharacterProfessionProvider.KEY);
                    if (professions == null) {
                        professions = new ProfessionData();
                        data.set(CharacterProfessionProvider.KEY, professions);
                    }

                    int oldLevel = getLevel(professions.getExperience(type));
                    professions.addExperience(type, amount);
                    data.markDirty(CharacterProfessionProvider.KEY);
                    characterDataRepository.save(character.characterId(), data);

                    ProfessionProgress progress = getProgress(player, type);
                    showProgress(player, progress);
                    if (progress.level() > oldLevel) {
                        showLevelUp(player, type, oldLevel, progress.level());
                    }
                }));
    }

    public int getLevel(long totalExperience) {
        long remaining = Math.max(0L, totalExperience);
        int level = 1;
        while (level < MAX_LEVEL) {
            long required = getExperienceForNextLevel(level);
            if (remaining < required) {
                break;
            }
            remaining -= required;
            level++;
        }
        return level;
    }

    public long getExperienceForNextLevel(int level) {
        if (level >= MAX_LEVEL) {
            return 0L;
        }
        return Math.max(1L, (long) (BASE_EXP * Math.pow(Math.max(1, level), 1.1D)));
    }

    private long getTotalExperienceForLevel(int level) {
        long total = 0L;
        for (int current = 1; current < level; current++) {
            total += getExperienceForNextLevel(current);
        }
        return total;
    }

    private void showProgress(Player player, ProfessionProgress progress) {
        BossBarState previous = bossBars.remove(player.getUniqueId());
        if (previous != null) {
            previous.hideTask().cancel();
            player.hideBossBar(previous.bossBar());
        }

        float ratio = progress.requiredExperience() <= 0L
                ? 1.0f
                : Math.min(1.0f, (float) progress.currentExperience() / progress.requiredExperience());
        Component title = Component.text(progress.type().getDisplayName() + " ", NamedTextColor.WHITE)
                .append(Component.text("Lv." + progress.level(), NamedTextColor.YELLOW))
                .append(Component.text(" [", NamedTextColor.GRAY))
                .append(Component.text(
                        progress.requiredExperience() <= 0L
                                ? "MAX"
                                : progress.currentExperience() + "/" + progress.requiredExperience(),
                        NamedTextColor.WHITE))
                .append(Component.text("]", NamedTextColor.GRAY));
        BossBar bossBar = BossBar.bossBar(title, ratio, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);

        BukkitTask hideTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.hideBossBar(bossBar);
            bossBars.remove(player.getUniqueId());
        }, BOSS_BAR_DURATION_TICKS);
        bossBars.put(player.getUniqueId(), new BossBarState(bossBar, hideTask));
    }

    private void showLevelUp(Player player, ProfessionType type, int oldLevel, int newLevel) {
        player.showTitle(Title.title(
                Component.text("PROFESSION LEVEL UP!", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(type.getDisplayName() + " Lv." + oldLevel + " → " + newLevel, NamedTextColor.YELLOW)
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.7f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.1f);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBossBar(event.getPlayer());
    }

    @Override
    public void stop() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeBossBar(player);
        }
        bossBars.clear();
    }

    private void removeBossBar(Player player) {
        BossBarState state = bossBars.remove(player.getUniqueId());
        if (state != null) {
            state.hideTask().cancel();
            player.hideBossBar(state.bossBar());
        }
    }

    public record ProfessionProgress(
            ProfessionType type,
            int level,
            long totalExperience,
            long currentExperience,
            long requiredExperience) {
    }

    private record BossBarState(BossBar bossBar, BukkitTask hideTask) {
    }
}
