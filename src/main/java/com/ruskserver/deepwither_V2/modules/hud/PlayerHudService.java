package com.ruskserver.deepwither_V2.modules.hud;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.database.character.CharacterData;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.combat.health.MagicShieldManager;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.combat.health.event.VirtualHealthChangeEvent;
import com.ruskserver.deepwither_V2.modules.hud.EoaHudLayout.HudSnapshot;
import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.SkillBarSnapshot;
import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.SkillSlotSnapshot;
import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.SlotState;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.provider.CharacterSkillSlotProvider;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillCastService;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillCooldownService;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillRegistry;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillSessionService;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BetterHUD を置き換える自前 HUD 管理サービス。
 * <p>
 * プレイヤーごとに HUD 専用の透明 BossBar ({@link BossBar.Color#PINK}) を割り当て、
 * 仮想HP・バリア・マジックシールド・マナを {@code eoa:hud} フォントで描画する。
 * Fキーのスキルモード中は、同じ BossBar 内に 9 スロットのスキルバー（全スキル専用アイコン＋CDワイプ＋秒数）も重ねて描画する。
 */
@Service
public class PlayerHudService implements Startable, Stoppable, Listener {

    private static final long UPDATE_INTERVAL_TICKS = 2L;

    private final Deepwither_V2 plugin;
    private final VirtualHealthManager healthManager;
    private final MagicShieldManager magicShieldManager;
    private final ManaManager manaManager;
    private final SkillSessionService skillSessionService;
    private final SkillRegistry skillRegistry;
    private final SkillCastService skillCastService;
    private final SkillCooldownService cooldownService;
    private final SkillTreeService skillTreeService;
    private final CharacterService characterService;
    private final CharacterDataRepository characterDataRepository;

    private final Map<UUID, PlayerHudState> states = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    @Inject
    public PlayerHudService(
            Deepwither_V2 plugin,
            VirtualHealthManager healthManager,
            MagicShieldManager magicShieldManager,
            ManaManager manaManager,
            SkillSessionService skillSessionService,
            SkillRegistry skillRegistry,
            SkillCastService skillCastService,
            SkillCooldownService cooldownService,
            SkillTreeService skillTreeService,
            CharacterService characterService,
            CharacterDataRepository characterDataRepository
    ) {
        this.plugin = plugin;
        this.healthManager = healthManager;
        this.magicShieldManager = magicShieldManager;
        this.manaManager = manaManager;
        this.skillSessionService = skillSessionService;
        this.skillRegistry = skillRegistry;
        this.skillCastService = skillCastService;
        this.cooldownService = cooldownService;
        this.skillTreeService = skillTreeService;
        this.characterService = characterService;
        this.characterDataRepository = characterDataRepository;
    }

    @Override
    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            attach(player);
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);
    }

    @Override
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (Map.Entry<UUID, PlayerHudState> entry : states.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && entry.getValue().visible) {
                player.hideBossBar(entry.getValue().bossBar);
            }
        }
        states.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        attach(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        detach(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                refresh(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVirtualHealthChange(VirtualHealthChangeEvent event) {
        if (event.getEntity() instanceof Player player && player.isOnline()) {
            refresh(player);
        }
    }

    /** 即時更新が必要な処理（マナ消費・バリア変化・スキルモード切替など）から呼び出せる公開メソッド */
    public void refresh(Player player) {
        PlayerHudState state = states.get(player.getUniqueId());
        if (state == null) return;

        boolean shouldShow = player.getGameMode() != GameMode.SPECTATOR;
        if (!shouldShow) {
            if (state.visible) {
                player.hideBossBar(state.bossBar);
                state.visible = false;
            }
            return;
        }

        if (!state.visible) {
            player.showBossBar(state.bossBar);
            state.visible = true;
        }

        HudSnapshot snapshot = HudSnapshot.of(
                healthManager.getHealth(player),
                healthManager.getMaxHealth(player),
                manaManager.getMana(player),
                manaManager.getMaxMana(player),
                healthManager.getBarrier(player),
                magicShieldManager.getMagicShield(player),
                magicShieldManager.getMaxMagicShield(player)
        );
        SkillBarSnapshot skillBar = skillSessionService.isInSkillMode(player)
                ? buildSkillBarSnapshot(player)
                : null;

        if (!snapshot.equals(state.lastSnapshot) || !Objects.equals(skillBar, state.lastSkillBar)) {
            state.lastSnapshot = snapshot;
            state.lastSkillBar = skillBar;
            state.bossBar.name(EoaHudRenderer.render(snapshot, skillBar));
        }
    }

    private SkillBarSnapshot buildSkillBarSnapshot(Player player) {
        Optional<CharacterData> dataOpt = characterService.getCachedActiveCharacter(player.getUniqueId())
                .flatMap(character -> characterDataRepository.get(character.characterId()));
        CharacterSkillSlotProvider.SkillSlotData slotData = dataOpt
                .map(data -> data.get(CharacterSkillSlotProvider.KEY))
                .orElse(null);

        Skill castingSkill = skillCastService.getCastingSkill(player);
        String castingId = castingSkill != null ? castingSkill.getId() : null;
        double currentMana = manaManager.getMana(player);
        UUID playerId = player.getUniqueId();

        List<SkillSlotSnapshot> slots = new ArrayList<>(EoaSkillBarLayout.DISPLAY_SLOT_COUNT);
        boolean[] usedKey = new boolean[CharacterSkillSlotProvider.SLOT_COUNT];

        // 装備済みスキル (最大 4 個) を実際のホットバーキー番号付きで収集
        for (int i = 0; i < CharacterSkillSlotProvider.SLOT_COUNT && slots.size() < EoaSkillBarLayout.DISPLAY_SLOT_COUNT; i++) {
            String skillId = slotData != null ? slotData.getSkill(i) : null;
            if (skillId == null || skillId.isBlank()) continue;

            Skill skill = skillRegistry.get(skillId);
            if (skill == null) continue;

            usedKey[i] = true;
            int keyNumber = i + 1;

            if (!skillTreeService.isSkillUnlocked(player, skillId)) {
                slots.add(SkillSlotSnapshot.locked(keyNumber, skillId));
                continue;
            }

            if (skillId.equals(castingId)) {
                slots.add(new SkillSlotSnapshot(keyNumber, skillId, SlotState.CASTING, 0, ""));
                continue;
            }

            if (cooldownService.isOnCooldown(playerId, skillId)) {
                long remMs = cooldownService.getRemaining(playerId, skillId).toMillis();
                long totalMs = cooldownService.getTotalDuration(playerId, skillId).toMillis();
                if (totalMs <= 0L) {
                    SkillContext ctx = new SkillContext(
                            player, skill, skillTreeService.getSkillLevel(player, skillId), manaManager, cooldownService);
                    Duration baseCd = skill.getCooldown(ctx);
                    totalMs = baseCd != null ? baseCd.toMillis() : remMs;
                }
                slots.add(SkillSlotSnapshot.cooldown(keyNumber, skillId, remMs, totalMs));
                continue;
            }

            SkillContext ctx = new SkillContext(
                    player, skill, skillTreeService.getSkillLevel(player, skillId), manaManager, cooldownService);
            if (currentMana < skill.getManaCost(ctx)) {
                slots.add(new SkillSlotSnapshot(keyNumber, skillId, SlotState.NO_MANA, 0, ""));
            } else {
                slots.add(new SkillSlotSnapshot(keyNumber, skillId, SlotState.READY, 0, ""));
            }
        }

        // 4 スロット未満の時は未使用の 1..4 番スロットを空枠で補完
        for (int i = 0; i < CharacterSkillSlotProvider.SLOT_COUNT && slots.size() < EoaSkillBarLayout.DISPLAY_SLOT_COUNT; i++) {
            if (!usedKey[i]) {
                slots.add(SkillSlotSnapshot.empty(i + 1));
            }
        }
        return new SkillBarSnapshot(List.copyOf(slots));
    }

    private void attach(Player player) {
        PlayerHudState state = states.computeIfAbsent(player.getUniqueId(), id -> new PlayerHudState(
                BossBar.bossBar(Component.empty(), 0.0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS)
        ));
        refresh(player);
        if (!state.visible && player.getGameMode() != GameMode.SPECTATOR) {
            player.showBossBar(state.bossBar);
            state.visible = true;
        }
    }

    private void detach(Player player) {
        PlayerHudState state = states.remove(player.getUniqueId());
        if (state != null && state.visible) {
            player.hideBossBar(state.bossBar);
        }
    }

    private void tickAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private static final class PlayerHudState {
        final BossBar bossBar;
        boolean visible;
        HudSnapshot lastSnapshot;
        SkillBarSnapshot lastSkillBar;

        PlayerHudState(BossBar bossBar) {
            this.bossBar = bossBar;
        }
    }
}
