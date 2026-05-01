package com.ruskserver.deepwither_V2.modules.skill.service;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillCastAttemptEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillCastCancelEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillCastCompleteEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillCastStartEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillCooldownApplyEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillExecuteEvent;
import com.ruskserver.deepwither_V2.modules.skill.event.SkillManaConsumeEvent;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SkillCastService implements Stoppable {

    private final Deepwither_V2 plugin;
    private final ManaManager manaManager;
    private final SkillCooldownService cooldownService;
    private final SkillTreeService skillTreeService;
    private final Map<UUID, CastingState> casting = new HashMap<>();

    @Inject
    public SkillCastService(Deepwither_V2 plugin, ManaManager manaManager, SkillCooldownService cooldownService, SkillTreeService skillTreeService) {
        this.plugin = plugin;
        this.manaManager = manaManager;
        this.cooldownService = cooldownService;
        this.skillTreeService = skillTreeService;
    }

    @Override
    public void stop() {
        casting.values().forEach(state -> state.task.cancel());
        casting.clear();
    }

    public boolean isCasting(Player player) {
        return casting.containsKey(player.getUniqueId());
    }

    public Skill getCastingSkill(Player player) {
        CastingState state = casting.get(player.getUniqueId());
        return state == null ? null : state.skill;
    }

    public void cast(Player player, Skill skill) {
        SkillCastAttemptEvent attemptEvent = new SkillCastAttemptEvent(player, skill);
        Bukkit.getPluginManager().callEvent(attemptEvent);
        if (attemptEvent.isCancelled()) {
            return;
        }

        SkillContext context = createContext(player, skill);
        if (!canCast(player, skill, context, true)) {
            return;
        }

        Duration castTime = sanitize(skill.getCastTime(context));
        if (castTime.isZero()) {
            execute(player, skill);
            return;
        }

        SkillCastStartEvent startEvent = new SkillCastStartEvent(player, skill, castTime);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            return;
        }

        startCasting(player, skill, sanitize(startEvent.getCastTime()));
    }

    public void cancelCast(Player player) {
        UUID uuid = player.getUniqueId();
        CastingState state = casting.remove(uuid);
        if (state == null) return;

        state.task.cancel();
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        Bukkit.getPluginManager().callEvent(new SkillCastCancelEvent(player, state.skill));
        player.sendMessage(Component.text("詠唱をキャンセルしました。", NamedTextColor.GRAY));
    }

    private void startCasting(Player player, Skill skill, Duration castTime) {
        UUID uuid = player.getUniqueId();
        long ticks = Math.max(1L, castTime.toMillis() / 50L);

        player.sendMessage(Component.text("詠唱開始: " + skill.getDisplayName(), NamedTextColor.YELLOW));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) ticks + 5, 3, false, false));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            casting.remove(uuid);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            if (!player.isOnline()) return;
            execute(player, skill);
        }, ticks);

        casting.put(uuid, new CastingState(skill, task));
    }

    private boolean canCast(Player player, Skill skill, SkillContext context, boolean notify) {
        if (!skillTreeService.isSkillUnlocked(player, skill.getId())) {
            if (notify) player.sendMessage(Component.text("このスキルはまだ解放されていません。", NamedTextColor.RED));
            return false;
        }

        if (isCasting(player)) {
            if (notify) player.sendMessage(Component.text("詠唱中です。", NamedTextColor.RED));
            return false;
        }

        double manaCost = Math.max(0.0, skill.getManaCost(context));
        if (manaManager.getMana(player) < manaCost) {
            if (notify) player.sendMessage(Component.text("マナが足りません。", NamedTextColor.RED));
            return false;
        }

        if (cooldownService.isOnCooldown(player.getUniqueId(), skill.getId())) {
            double seconds = cooldownService.getRemaining(player.getUniqueId(), skill.getId()).toMillis() / 1000.0;
            if (notify) {
                player.sendMessage(Component.text(String.format("クールダウン中です。(残り %.1f 秒)", seconds), NamedTextColor.YELLOW));
            }
            return false;
        }

        return true;
    }

    private void execute(Player player, Skill skill) {
        SkillContext context = createContext(player, skill);
        if (!canCast(player, skill, context, true)) {
            return;
        }

        SkillExecuteEvent executeEvent = new SkillExecuteEvent(player, skill);
        Bukkit.getPluginManager().callEvent(executeEvent);
        if (executeEvent.isCancelled()) {
            return;
        }

        CastResult result = skill.cast(context);
        if (result == null || !result.isSuccess()) {
            if (result != null && result.getMessage() != null) {
                player.sendMessage(result.getMessage());
            } else {
                player.sendMessage(Component.text("発動条件を満たしていません。", NamedTextColor.GRAY));
            }
            return;
        }

        if (!consumeManaAfterSuccess(player, skill, context)) {
            player.sendMessage(Component.text("マナが不足したため発動を中断しました。", NamedTextColor.RED));
            return;
        }
        applyCooldownAfterSuccess(player, skill, context);

        Bukkit.getPluginManager().callEvent(new SkillCastCompleteEvent(player, skill));
        player.sendMessage(Component.text("スキル「" + skill.getDisplayName() + "」を発動！", NamedTextColor.GREEN));
    }

    private boolean consumeManaAfterSuccess(Player player, Skill skill, SkillContext context) {
        double amount = Math.max(0.0, skill.getManaCost(context));
        SkillManaConsumeEvent event = new SkillManaConsumeEvent(player, skill, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        double finalAmount = Math.max(0.0, event.getAmount());
        return finalAmount <= 0.0 || manaManager.consume(player, finalAmount);
    }

    private void applyCooldownAfterSuccess(Player player, Skill skill, SkillContext context) {
        Duration cooldown = sanitize(skill.getCooldown(context));
        SkillCooldownApplyEvent event = new SkillCooldownApplyEvent(player, skill, cooldown);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            cooldownService.applyCooldown(player.getUniqueId(), skill.getId(), sanitize(event.getCooldown()));
        }
    }

    private SkillContext createContext(Player player, Skill skill) {
        return new SkillContext(player, skill, skillTreeService.getSkillLevel(player, skill.getId()), manaManager, cooldownService);
    }

    private Duration sanitize(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }

    private record CastingState(Skill skill, BukkitTask task) {
    }
}
