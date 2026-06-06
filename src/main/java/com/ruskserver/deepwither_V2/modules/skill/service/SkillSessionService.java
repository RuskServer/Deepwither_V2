package com.ruskserver.deepwither_V2.modules.skill.service;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.database.character.CharacterData;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.skill.provider.CharacterSkillSlotProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class SkillSessionService implements Startable, Stoppable {

    private final Deepwither_V2 plugin;
    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final SkillRegistry registry;
    private final SkillCastService castService;
    private final SkillCooldownService cooldownService;
    private final ManaManager manaManager;
    private final SkillTreeService skillTreeService;
    private final Set<UUID> skillModePlayers = new HashSet<>();
    private BukkitTask actionBarTask;

    @Inject
    public SkillSessionService(
            Deepwither_V2 plugin,
            CharacterDataRepository characterDataRepository,
            CharacterService characterService,
            SkillRegistry registry,
            SkillCastService castService,
            SkillCooldownService cooldownService,
            ManaManager manaManager,
            SkillTreeService skillTreeService
    ) {
        this.plugin = plugin;
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.registry = registry;
        this.castService = castService;
        this.cooldownService = cooldownService;
        this.manaManager = manaManager;
        this.skillTreeService = skillTreeService;
    }

    @Override
    public void start() {
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateActionBars, 0L, 10L);
    }

    @Override
    public void stop() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        skillModePlayers.clear();
    }

    public boolean isInSkillMode(Player player) {
        return skillModePlayers.contains(player.getUniqueId());
    }

    public void toggleSkillMode(Player player) {
        if (isInSkillMode(player)) {
            exitSkillMode(player);
        } else {
            enterSkillMode(player);
        }
    }

    public void enterSkillMode(Player player) {
        skillModePlayers.add(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 2.0f);
    }

    public void exitSkillMode(Player player) {
        skillModePlayers.remove(player.getUniqueId());
        castService.cancelCast(player);
        player.sendActionBar(Component.empty());
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 2.0f);
    }

    public void castSlot(Player player, int slot) {
        getActiveSkillData(player).ifPresent(data -> {
            CharacterSkillSlotProvider.SkillSlotData slotData = data.get(CharacterSkillSlotProvider.KEY);
            if (slotData == null) return;

            String skillId = slotData.getSkill(slot);
            if (skillId == null) {
                player.sendMessage(Component.text("このスロットにはスキルが設定されていません。", NamedTextColor.RED));
                return;
            }

            Skill skill = registry.get(skillId);
            if (skill == null) {
                player.sendMessage(Component.text("スキル定義が見つかりません: " + skillId, NamedTextColor.RED));
                return;
            }

            castService.cast(player, skill);
        });
    }

    private void updateActionBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInSkillMode(player)) continue;

            Skill castingSkill = castService.getCastingSkill(player);
            if (castingSkill != null) {
                player.sendActionBar(Component.text("詠唱中: " + castingSkill.getDisplayName() + " (Fキーでキャンセル)", NamedTextColor.YELLOW));
                continue;
            }

            getActiveSkillData(player).ifPresent(data -> {
                CharacterSkillSlotProvider.SkillSlotData slotData = data.get(CharacterSkillSlotProvider.KEY);
                if (slotData == null) return;

                Component bar = Component.empty();
                for (int i = 0; i < CharacterSkillSlotProvider.SLOT_COUNT; i++) {
                    String skillId = slotData.getSkill(i);
                    if (skillId == null) continue;

                    Skill skill = registry.get(skillId);
                    Component prefix = Component.text("[" + (i + 1) + "] ", NamedTextColor.GRAY);
                    if (skill == null) {
                        bar = bar.append(prefix).append(Component.text("ERROR  ", NamedTextColor.RED));
                        continue;
                    }

                    if (!skillTreeService.isSkillUnlocked(player, skill.getId())) {
                        bar = bar.append(prefix).append(Component.text("LOCKED  ", NamedTextColor.DARK_GRAY));
                        continue;
                    }

                    SkillContext context = new SkillContext(player, skill, skillTreeService.getSkillLevel(player, skill.getId()), manaManager, cooldownService);
                    Component display = getActionBarDisplay(player, skill, context);
                    bar = bar.append(prefix).append(display).append(Component.text("  "));
                }
                player.sendActionBar(bar);
            });
        }
    }

    private Component getActionBarDisplay(Player player, Skill skill, SkillContext context) {
        if (cooldownService.isOnCooldown(player.getUniqueId(), skill.getId())) {
            double seconds = cooldownService.getRemaining(player.getUniqueId(), skill.getId()).toMillis() / 1000.0;
            return Component.text(skill.getDisplayName(), NamedTextColor.RED)
                    .append(Component.text(String.format(" (%.1fs)", seconds), NamedTextColor.DARK_RED));
        }
        if (manaManager.getMana(player) < skill.getManaCost(context)) {
            return Component.text("x " + skill.getDisplayName(), NamedTextColor.BLUE);
        }
        return Component.text(skill.getDisplayName(), NamedTextColor.GREEN, TextDecoration.BOLD);
    }

    private java.util.Optional<CharacterData> getActiveSkillData(Player player) {
        return characterService.getCachedActiveCharacter(player.getUniqueId())
                .flatMap(character -> characterDataRepository.get(character.characterId()));
    }
}
