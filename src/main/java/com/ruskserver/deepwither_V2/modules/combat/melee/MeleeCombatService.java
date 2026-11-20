package com.ruskserver.deepwither_V2.modules.combat.melee;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.feedback.DamageFeedbackService;
import com.ruskserver.deepwither_V2.modules.combat.melee.MeleeGuardStateService.GuardOutcome;
import com.ruskserver.deepwither_V2.modules.combat.melee.MeleeWeaponArchetype.ComboStep;
import com.ruskserver.deepwither_V2.modules.combat.stagger.BossStaggerService;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillSessionService;
import com.ruskserver.deepwither_V2.modules.skill.util.AnimeVfxHelper;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ソウルライク / Dark and Darker 風の近接戦闘エンジン。
 * <ul>
 *   <li>全 7 近接武器種ごとの 2〜3 段コンボ（振りかぶり → 実体スイング判定 → 戻り）</li>
 *   <li>三人称 {@link MeleeDisplayAnimator}（プレイヤー騎乗 ItemDisplay 60fps 補間）</li>
 *   <li>ヒットストップ（1〜2 tick 停止）＋ 32×32 三日月斬撃 / 突きビーム / 地面衝撃波 / 2方向クロス斬線</li>
 *   <li>右クリックガード ＆ ジャストパリィ（0.25s 窓）＋ 弾かれ硬直アニメーション ＋ リポスト（1.6x）</li>
 * </ul>
 */
@Service
public class MeleeCombatService implements Startable, Stoppable, Listener {

    private static final long COMBO_RESET_MILLIS = 1450L;
    private static final Vector UP = new Vector(0, 1, 0);

    private final Deepwither_V2 plugin;
    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;
    private final StatManager statManager;
    private final DamagePipelineManager damagePipeline;
    private final DamageFeedbackService feedbackService;
    private final BossStaggerService staggerService;
    private final SkillSessionService skillSessionService;
    private final MeleeGuardStateService guardStateService;
    private final MeleeDisplayAnimator displayAnimator;
    private final MeleeFirstPersonAnimator fpAnimator;
    private final NamespacedKey noBobAttackSpeedKey;

    private final Map<UUID, ActiveSwing> activeSwings = new ConcurrentHashMap<>();
    private final Map<UUID, ComboProgress> comboMap = new ConcurrentHashMap<>();
    private final Map<UUID, RecoilState> activeRecoils = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    @Inject
    public MeleeCombatService(
            Deepwither_V2 plugin,
            ItemManager itemManager,
            ItemPDCUtil pdcUtil,
            StatManager statManager,
            DamagePipelineManager damagePipeline,
            DamageFeedbackService feedbackService,
            BossStaggerService staggerService,
            SkillSessionService skillSessionService,
            MeleeGuardStateService guardStateService) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
        this.statManager = statManager;
        this.damagePipeline = damagePipeline;
        this.feedbackService = feedbackService;
        this.staggerService = staggerService;
        this.skillSessionService = skillSessionService;
        this.guardStateService = guardStateService;
        this.displayAnimator = new MeleeDisplayAnimator(plugin);
        this.fpAnimator = new MeleeFirstPersonAnimator();
        this.noBobAttackSpeedKey = new NamespacedKey(plugin, "melee_no_bob_attack_speed");
    }

    @Override
    public void start() {
        guardStateService.setListeners(this::handleDefenderGuardOutcome, this::handleAttackerRecoil);
        fpAnimator.start();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
        for (Player p : Bukkit.getOnlinePlayers()) {
            syncHeldWeaponNoBob(p, p.getInventory().getHeldItemSlot());
        }
    }

    @Override
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        fpAnimator.stop();
        activeSwings.clear();
        comboMap.clear();
        activeRecoils.clear();
        displayAnimator.clearAll();
        for (Player p : Bukkit.getOnlinePlayers()) {
            MeleeFirstPersonAnimator.clearPoseOnSlot(p, p.getInventory().getHeldItemSlot());
            removeNoBobAttackSpeed(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem customItem = resolveCustomItem(mainHand);
        if (customItem == null) return;
        MeleeWeaponArchetype archetype = MeleeWeaponArchetype.fromWeaponType(customItem.getWeaponType());
        if (archetype == null) return;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            tryStartSwing(player, mainHand, customItem, archetype);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            tryRaiseGuard(player, mainHand, archetype);
        }
    }

    /**
     * カスタム近接武器でモブを直接左クリックしたときも、バニラ即ヒットではなく
     * ソウルライクコンボスイングへ統一する。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDirectEntityClick(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        CustomItem customItem = resolveCustomItem(mainHand);
        if (customItem == null) return;
        MeleeWeaponArchetype archetype = MeleeWeaponArchetype.fromWeaponType(customItem.getWeaponType());
        if (archetype == null) return;

        event.setCancelled(true);
        event.setDamage(0.0);
        tryStartSwing(player, mainHand, customItem, archetype);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                syncHeldWeaponNoBob(player, player.getInventory().getHeldItemSlot());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSlotSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        activeSwings.remove(id);
        activeRecoils.remove(id);
        guardStateService.clearGuard(id);
        displayAnimator.remove(id);
        fpAnimator.stopAndRestore(player);
        MeleeFirstPersonAnimator.clearPoseOnSlot(player, event.getPreviousSlot());
        syncHeldWeaponNoBob(player, event.getNewSlot());
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        MeleeFirstPersonAnimator.clearPoseOnStack(event.getItemDrop().getItemStack());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        activeSwings.remove(id);
        comboMap.remove(id);
        activeRecoils.remove(id);
        guardStateService.remove(id);
        displayAnimator.remove(id);
        fpAnimator.cancelWithoutRestore(id);
        removeNoBobAttackSpeed(event.getPlayer());
    }

    private void syncHeldWeaponNoBob(Player player, int slot) {
        ItemStack stack = (slot >= 0 && slot < player.getInventory().getSize())
                ? player.getInventory().getItem(slot)
                : null;
        CustomItem ci = resolveCustomItem(stack);
        if (ci != null && MeleeWeaponArchetype.fromWeaponType(ci.getWeaponType()) != null) {
            ensureNoBobAttackSpeed(player);
            MeleeFirstPersonAnimator.prepareWeaponFor1P(player, slot);
        } else {
            removeNoBobAttackSpeed(player);
        }
    }

    private void tryStartSwing(Player player, ItemStack weaponItem, CustomItem customItem, MeleeWeaponArchetype archetype) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (guardStateService.isStaggerLocked(playerId, now)) {
            return;
        }
        if (activeSwings.containsKey(playerId)) {
            return;
        }

        // ガード中に攻撃したらガードを解いてスイング開始
        guardStateService.clearGuard(playerId);

        ComboProgress progress = comboMap.get(playerId);
        int stepIndex = 0;
        if (progress != null
                && progress.archetype == archetype
                && now - progress.lastFinishedMillis <= COMBO_RESET_MILLIS) {
            stepIndex = (progress.lastStepIndex + 1) % archetype.stepCount();
        }

        ComboStep step = archetype.getStep(stepIndex);
        double atkSpeed = Math.max(0.45, Math.min(2.6, statManager.getTotalStat(player, StatType.ATTACK_SPEED)));
        double speedScale = 1.0 / Math.max(0.55, atkSpeed);
        int windup = Math.max(3, (int) Math.round(step.windupTicks() * speedScale));
        int active = Math.max(4, step.activeTicks());
        int recovery = Math.max(4, (int) Math.round(step.recoveryTicks() * speedScale));

        Color vfxColor = resolveWeaponVfxColor(customItem, archetype);
        ActiveSwing swing = new ActiveSwing(
                playerId,
                weaponItem.clone(),
                archetype,
                stepIndex,
                step,
                vfxColor,
                windup,
                active,
                recovery
        );
        activeSwings.put(playerId, swing);

        // バニラ攻撃ゲージ低下による mainHandHeight 落下（持ち替えボブ）を防止するため ATTACK_SPEED を引き上げ
        ensureNoBobAttackSpeed(player);

        // 1P 32フレーム区分フェーズ同期再生＋3P 刃先方向一致の振りかぶり＋重厚な予備動作SE
        String fpSeq = MeleeFirstPersonAnimator.swingSequenceName(archetype, stepIndex, step);
        fpAnimator.playPhasedSwing(player, fpSeq, MeleeFirstPersonAnimator.SWING_FRAME_COUNT,
                windup * 50L, active * 50L, recovery * 50L);
        apply3PSwingPhase(player, swing, fpSeq, SwingPhase3P.WINDUP, windup);
        MeleeSoundService.playWindup(player.getLocation(), archetype);
    }

    private void tryRaiseGuard(Player player, ItemStack weaponItem, MeleeWeaponArchetype archetype) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (activeSwings.containsKey(playerId) || guardStateService.isStaggerLocked(playerId, now)) {
            return;
        }

        boolean openedParryWindow = guardStateService.raiseOrRefreshGuard(playerId, now);
        ensureNoBobAttackSpeed(player);
        if (openedParryWindow) {
            fpAnimator.playSequence(player, "guard", MeleeFirstPersonAnimator.GUARD_FRAME_COUNT, 150L, true);
        }
        // 3P ガード構え: 胸前で斜め横に構えて刃を受ける
        displayAnimator.applyPoseQuat(player, weaponItem, 0.05f, 1.24f, 0.56f,
                MeleeDisplayAnimator.slash3PQuat(-48f, 22f, 24f, 45f), 2, scaleFor(archetype));
        if (openedParryWindow) {
            MeleeSoundService.playGuardRaise(player.getLocation());
            AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 1.15, Color.fromRGB(165, 230, 255), 8, 9.0, false);
        }
    }

    private void tickAll() {
        long now = System.currentTimeMillis();
        fpAnimator.tick();

        // 1. 弾かれ硬直／パリィ弾き（Recoil）更新（1P は 30fps パケット側で自動進行）
        Iterator<Map.Entry<UUID, RecoilState>> recIt = activeRecoils.entrySet().iterator();
        while (recIt.hasNext()) {
            Map.Entry<UUID, RecoilState> entry = recIt.next();
            RecoilState rec = entry.getValue();
            Player p = Bukkit.getPlayer(entry.getKey());
            rec.remainingTicks--;
            if (p == null || !p.isOnline() || p.isDead() || rec.remainingTicks <= 0) {
                recIt.remove();
                if (p != null && !activeSwings.containsKey(p.getUniqueId()) && !guardStateService.isGuarding(p.getUniqueId(), now)) {
                    displayAnimator.remove(p.getUniqueId());
                    fpAnimator.stopAndRestore(p);
                }
            } else {
                displayAnimator.syncYaw(p);
            }
        }

        // 2. ガード維持チェック（終了したら 1P/3P とも片付け）
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (guardStateService.isGuarding(id, now)) {
                displayAnimator.syncYaw(p);
            } else if (!activeSwings.containsKey(id) && !activeRecoils.containsKey(id)) {
                displayAnimator.remove(id);
                fpAnimator.stopAndRestore(p);
            }
        }

        // 3. アクティブなコンボスイング更新
        Iterator<Map.Entry<UUID, ActiveSwing>> it = activeSwings.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveSwing> entry = it.next();
            ActiveSwing swing = entry.getValue();
            Player player = Bukkit.getPlayer(swing.playerId);
            if (player == null || !player.isOnline() || player.isDead()) {
                it.remove();
                displayAnimator.remove(swing.playerId);
                fpAnimator.cancelWithoutRestore(swing.playerId);
                continue;
            }

            // ヒットストップ中は 30fps 1P フレーム進行もピタッと止める（手応え演出！）
            if (swing.hitstopTicks > 0) {
                swing.hitstopTicks--;
                fpAnimator.setPaused(swing.playerId, true);
                displayAnimator.syncYaw(player);
                continue;
            }
            fpAnimator.setPaused(swing.playerId, false);

            swing.tick++;
            int windupEnd = swing.windupTicks;
            int activeEnd = windupEnd + swing.activeTicks;
            int totalEnd = activeEnd + swing.recoveryTicks;

            int halfActive = Math.max(1, swing.activeTicks / 2);
            String fpSeq = MeleeFirstPersonAnimator.swingSequenceName(swing.archetype, swing.stepIndex, swing.step);
            if (swing.tick == windupEnd) {
                // Active Phase 開始: 踏み込み＋3P 前方中央中間ポーズへ（>180° の背後 SLERP 逆回り防止！）＋VFX＋スイング音
                beginActiveSwing(player, swing, fpSeq, halfActive);
            } else if (swing.tick == windupEnd + halfActive && swing.tick < activeEnd) {
                // Active Phase 後半: 3P 振り抜き終端へ 60fps 補間
                apply3PSwingPhase(player, swing, fpSeq, SwingPhase3P.ACTIVE_END, Math.max(1, activeEnd - swing.tick));
            }
            if (swing.tick == activeEnd - 1 && swing.step.groundShockwave()) {
                // 叩きつけの刃先が地面に到達する瞬間（1P=0.56）に合わせて衝撃波を発火
                triggerGroundSlamShockwave(player, swing);
            }
            if (swing.tick >= windupEnd && swing.tick < activeEnd) {
                performActiveHitDetection(player, swing);
                displayAnimator.syncYaw(player);
            } else if (swing.tick == activeEnd) {
                // Recovery Phase: 3P も戻りポーズ
                apply3PSwingPhase(player, swing, fpSeq, SwingPhase3P.RECOVERY, Math.max(2, swing.recoveryTicks));
            } else if (swing.tick >= totalEnd) {
                comboMap.put(swing.playerId, new ComboProgress(swing.archetype, swing.stepIndex, now));
                it.remove();
                if (!guardStateService.isGuarding(swing.playerId, now) && !activeRecoils.containsKey(swing.playerId)) {
                    displayAnimator.remove(swing.playerId);
                    fpAnimator.stopAndRestore(player);
                }
            } else {
                displayAnimator.syncYaw(player);
            }
        }
    }

    private void beginActiveSwing(Player player, ActiveSwing swing, String fpSeq, int halfActiveTicks) {
        ComboStep step = swing.step;

        // 3P ItemDisplay: 刃先を進行方向へ揃えた単一斬撃平面で正面中央の中間ポーズへ 60fps 補間
        apply3PSwingPhase(player, swing, fpSeq, SwingPhase3P.ACTIVE_MID, Math.max(1, halfActiveTicks));

        Vector forward = player.getEyeLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 1e-6) forward = new Vector(0, 0, 1);
        else forward.normalize();

        // 軽い踏み込み（Dark and Darker のステップ感）
        if (step.stepForwardImpulse() > 0.0 && player.isOnGround()) {
            Vector vel = player.getVelocity();
            player.setVelocity(vel.add(forward.clone().multiply(step.stepForwardImpulse() * 0.45)));
        }

        // 武器種別＋コンボ段数＋空気裂きの多層スイングSE
        Location chest = player.getLocation().add(0, 1.15, 0);
        MeleeSoundService.playSwing(chest, swing.archetype, swing.stepIndex, "thrust".equals(fpSeq));

        // 1P アニメーションの軌道・視点ピッチに完全同期した 32×32 アニメ斬撃 / 突き / 衝撃波 VFX
        Vector lookDir = player.getEyeLocation().getDirection();
        if (lookDir.lengthSquared() < 1e-6) lookDir = forward.clone();
        else lookDir.normalize();
        Location slashOrigin = player.getEyeLocation().add(lookDir.clone().multiply(0.45)).add(0, -0.22, 0);

        if ("thrust".equals(fpSeq)) {
            AnimeVfxHelper.spawnEnergyBeam(plugin, slashOrigin, lookDir, step.reach(), 0.85, swing.vfxColor, Math.max(4, swing.activeTicks + 1));
            Location ringLoc = slashOrigin.clone().add(lookDir.clone().multiply(0.85));
            AnimeVfxHelper.spawnProjectileMuzzle(plugin, ringLoc, lookDir, 1.15, swing.vfxColor, Math.max(4, swing.activeTicks + 1));
        } else {
            boolean reverseSweep = "slash_lr".equals(fpSeq);
            double syncedRollDeg = "slam".equals(fpSeq) ? 90.0 : Math.abs(step.slashRollDeg());
            AnimeVfxHelper.spawnCrescentSlash(plugin, slashOrigin, lookDir, step.reach(),
                    swing.vfxColor, syncedRollDeg, reverseSweep, Math.max(3, swing.activeTicks));
        }
    }

    private void triggerGroundSlamShockwave(Player player, ActiveSwing swing) {
        Vector forward = player.getEyeLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 1e-6) forward = new Vector(0, 0, 1);
        else forward.normalize();
        Location slamLoc = player.getLocation().add(forward.clone().multiply(swing.step.reach() * 0.65)).add(0, 0.12, 0);
        AnimeVfxHelper.spawnShockwave(plugin, slamLoc, 0.55, swing.step.reach() * 1.15, swing.vfxColor, 10, UP);
        MeleeSoundService.playGroundSlam(slamLoc);
    }

    private enum SwingPhase3P {
        WINDUP, ACTIVE_MID, ACTIVE_END, RECOVERY
    }

    /**
     * 1P・VFX と完全同一の単一斬撃平面＋刃先向き（{@code edgeTwist = ±85°}）で 3P {@link org.bukkit.entity.ItemDisplay} を駆動する。
     */
    private void apply3PSwingPhase(Player player, ActiveSwing swing, String fpSeq, SwingPhase3P phase, int interpTicks) {
        float scale = scaleFor(swing.archetype);
        float bankDeg = -(float) Math.abs(swing.step.slashRollDeg());
        switch (fpSeq) {
            case "thrust" -> {
                switch (phase) {
                    case WINDUP -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.32f, 1.16f, -0.25f, MeleeDisplayAnimator.slash3PQuat(0f, 0f, 84f, 0f), interpTicks, scale);
                    case ACTIVE_MID -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.20f, 1.16f, 0.65f, MeleeDisplayAnimator.slash3PQuat(0f, 0f, 86f, 0f), interpTicks, scale);
                    case ACTIVE_END -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.16f, 1.16f, 1.15f, MeleeDisplayAnimator.slash3PQuat(0f, 0f, 86f, 0f), interpTicks, scale);
                    case RECOVERY -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.34f, 1.14f, 0.32f, MeleeDisplayAnimator.slash3PQuat(0f, -8f, 32f, 0f), interpTicks, scale);
                }
            }
            case "slam" -> {
                switch (phase) {
                    case WINDUP -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.18f, 1.85f, -0.18f, MeleeDisplayAnimator.slash3PQuat(0f, 0f, -42f, 0f), interpTicks, scale);
                    case ACTIVE_MID -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.14f, 1.48f, 0.45f, MeleeDisplayAnimator.slash3PQuat(0f, 0f, 42f, 0f), interpTicks, scale);
                    case ACTIVE_END -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.14f, 0.78f, 0.68f, MeleeDisplayAnimator.slash3PQuat(0f, 0f, 108f, 0f), interpTicks, scale);
                    case RECOVERY -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.30f, 1.12f, 0.34f, MeleeDisplayAnimator.slash3PQuat(0f, -6f, 38f, 0f), interpTicks, scale);
                }
            }
            case "slash_lr" -> {
                switch (phase) {
                    case WINDUP -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            -0.52f, 1.06f, 0.18f, MeleeDisplayAnimator.slash3PQuat(bankDeg, 65f, 76f, -85f), interpTicks, scale);
                    case ACTIVE_MID -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.00f, 1.16f, 0.62f, MeleeDisplayAnimator.slash3PQuat(bankDeg, 0f, 78f, -85f), interpTicks, scale);
                    case ACTIVE_END -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.56f, 1.28f, 0.42f, MeleeDisplayAnimator.slash3PQuat(bankDeg, -68f, 74f, -85f), interpTicks, scale);
                    case RECOVERY -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.36f, 1.16f, 0.30f, MeleeDisplayAnimator.slash3PQuat(bankDeg * 0.4f, -24f, 34f, -35f), interpTicks, scale);
                }
            }
            default -> { // slash_rl (右高 -> 左低, edgeTwist = +85° で刃先が左斜め下先頭!)
                switch (phase) {
                    case WINDUP -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.56f, 1.28f, 0.16f, MeleeDisplayAnimator.slash3PQuat(bankDeg, -65f, 74f, 85f), interpTicks, scale);
                    case ACTIVE_MID -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            0.00f, 1.16f, 0.62f, MeleeDisplayAnimator.slash3PQuat(bankDeg, 0f, 78f, 85f), interpTicks, scale);
                    case ACTIVE_END -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            -0.54f, 1.04f, 0.42f, MeleeDisplayAnimator.slash3PQuat(bankDeg, 68f, 76f, 85f), interpTicks, scale);
                    case RECOVERY -> displayAnimator.applyPoseQuat(player, swing.weaponItem,
                            -0.30f, 1.12f, 0.30f, MeleeDisplayAnimator.slash3PQuat(bankDeg * 0.4f, 24f, 34f, 35f), interpTicks, scale);
                }
            }
        }
    }

    private void performActiveHitDetection(Player player, ActiveSwing swing) {
        ComboStep step = swing.step;
        Location origin = player.getLocation().add(0, 1.15, 0);
        Vector forward = player.getEyeLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 1e-6) forward = new Vector(0, 0, 1);
        else forward.normalize();

        double reach = step.reach();
        double reachSq = reach * reach;
        double minCos = Math.cos(Math.toRadians(step.arcDegrees() * 0.5));
        long now = System.currentTimeMillis();

        for (Entity entity : player.getNearbyEntities(reach, 2.4, reach)) {
            if (!(entity instanceof LivingEntity target) || target.equals(player) || target.isDead()) {
                continue;
            }
            if (!swing.hitEntities.add(target.getUniqueId())) {
                continue;
            }

            Vector toTarget = target.getLocation().toVector().subtract(origin.toVector()).setY(0);
            double distSq = toTarget.lengthSquared();
            if (distSq > reachSq) continue;
            if (distSq > 0.36 && toTarget.normalize().dot(forward) < minCos) continue;

            boolean isRiposte = guardStateService.consumeRiposteVulnerability(target.getUniqueId(), now);
            double coeff = step.damageMultiplier() * (isRiposte ? MeleeGuardStateService.RIPOSTE_DAMAGE_MULTIPLIER : 1.0);
            double staggerMult = step.staggerMultiplier() * (isRiposte ? 1.5 : 1.0);

            Location hitLoc = target.getLocation().add(0, target.getHeight() * 0.55, 0);
            damagePipeline.processScaledDamage(
                    player,
                    target,
                    DamageType.PHYSICAL,
                    coeff,
                    Set.of("melee", "melee_combo", swing.archetype.name().toLowerCase()),
                    "melee_combo:" + swing.archetype.name() + ":" + swing.stepIndex,
                    0L,
                    hitLoc,
                    staggerMult
            );

            // パリィで弾かれてスイングが強制キャンセルされたら即終了
            if (!activeSwings.containsKey(player.getUniqueId())) {
                return;
            }

            // ヒット演出: 武器種別の斬撃/刺突/打撃SE＋2方向クロス斬線（✕）＋ヒットストップ
            MeleeSoundService.playHit(hitLoc, swing.archetype, step.thrust(), isRiposte);
            AnimeVfxHelper.spawnHitSlashCross(plugin, hitLoc,
                    isRiposte ? Color.fromRGB(255, 225, 95) : swing.vfxColor,
                    isRiposte ? 3.4 : 2.6, 6);
            if (isRiposte) {
                AnimeVfxHelper.spawnImpactBurst(plugin, hitLoc, 2.0, Color.fromRGB(255, 230, 105), 6);
                player.sendActionBar(Component.text("★ RIPOSTE! (1.6x)", NamedTextColor.GOLD, TextDecoration.BOLD));
            }

            if (!swing.hitstopTriggered) {
                swing.hitstopTriggered = true;
                swing.hitstopTicks = (swing.archetype == MeleeWeaponArchetype.GREATSWORD
                        || swing.archetype == MeleeWeaponArchetype.HAMMER
                        || swing.archetype == MeleeWeaponArchetype.AXE) ? 2 : 1;
            }
        }
    }

    private void handleDefenderGuardOutcome(UUID defenderId, GuardOutcome outcome) {
        Player defender = Bukkit.getPlayer(defenderId);
        if (defender == null || !defender.isOnline()) return;

        ItemStack mainHand = defender.getInventory().getItemInMainHand();
        CustomItem customItem = resolveCustomItem(mainHand);
        MeleeWeaponArchetype archetype = customItem != null
                ? MeleeWeaponArchetype.fromWeaponType(customItem.getWeaponType())
                : MeleeWeaponArchetype.SWORD;
        float scale = scaleFor(archetype != null ? archetype : MeleeWeaponArchetype.SWORD);

        Location center = defender.getLocation().add(0, 1.25, 0);
        Vector dir = defender.getEyeLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 1e-6) dir = new Vector(0, 0, 1);
        else dir.normalize();
        Location clashLoc = center.clone().add(dir.clone().multiply(0.9));

        if (outcome == GuardOutcome.JUST_PARRY) {
            fpAnimator.playSequence(defender, "parry", MeleeFirstPersonAnimator.PARRY_FRAME_COUNT, 450L, false);
            displayAnimator.applyPoseQuat(defender, mainHand, 0.54f, 1.48f, 0.52f,
                    MeleeDisplayAnimator.slash3PQuat(28f, -45f, 36f, -60f), 2, scale);
            activeRecoils.put(defenderId, new RecoilState(10, "parry", MeleeFirstPersonAnimator.PARRY_FRAME_COUNT));
            AnimeVfxHelper.spawnImpactBurst(plugin, clashLoc, 1.9, Color.fromRGB(255, 232, 105), 7);
            AnimeVfxHelper.spawnShockwave(plugin, clashLoc, 0.25, 2.2, Color.fromRGB(255, 232, 105), 8, dir);
            AnimeVfxHelper.spawnShockwave(plugin, defender.getLocation().add(0, 0.10, 0),
                    0.4, 2.8, Color.fromRGB(255, 225, 110), 9, UP);
            MeleeSoundService.playJustParry(clashLoc);
            defender.sendActionBar(Component.text("★ JUST PARRY!", NamedTextColor.GOLD, TextDecoration.BOLD));
        } else if (outcome == GuardOutcome.GUARD_BLOCK) {
            MeleeSoundService.playGuardBlock(clashLoc);
            feedbackService.playBlockFeedback(defender);
            defender.sendActionBar(Component.text("🛡 GUARD", NamedTextColor.AQUA));
        }
    }

    /**
     * ガード / ジャストパリィで弾かれた攻撃側のアニメーション＆硬直処理。
     */
    private void handleAttackerRecoil(UUID attackerId, GuardOutcome outcome) {
        Entity entity = Bukkit.getEntity(attackerId);
        if (!(entity instanceof LivingEntity attacker) || !attacker.isValid()) return;

        // 進行中のスイングを即キャンセル
        activeSwings.remove(attackerId);

        if (outcome == GuardOutcome.JUST_PARRY) {
            // 後ろへノックバック＆よろけ
            Vector back = attacker.getLocation().getDirection().setY(0).multiply(-0.55).setY(0.18);
            attacker.setVelocity(back);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 22, 3, false, false));

            if (attacker instanceof Player attackerPlayer) {
                ItemStack held = attackerPlayer.getInventory().getItemInMainHand();
                CustomItem ci = resolveCustomItem(held);
                MeleeWeaponArchetype arch = ci != null ? MeleeWeaponArchetype.fromWeaponType(ci.getWeaponType()) : null;
                fpAnimator.playSequence(attackerPlayer, "recoil", MeleeFirstPersonAnimator.RECOIL_FRAME_COUNT, 900L, false);
                displayAnimator.applyPoseQuat(attackerPlayer, held, 0.62f, 1.66f, 0.12f,
                        MeleeDisplayAnimator.slash3PQuat(36f, -52f, -46f, 0f), 2,
                        scaleFor(arch != null ? arch : MeleeWeaponArchetype.SWORD));
                activeRecoils.put(attackerId, new RecoilState(18, "recoil", MeleeFirstPersonAnimator.RECOIL_FRAME_COUNT));
                MeleeSoundService.playParriedRecoil(attackerPlayer.getLocation());
                feedbackService.playHurtFeedback(attackerPlayer, 35f);
                attackerPlayer.sendActionBar(Component.text("✖ PARRIED! (硬直)", NamedTextColor.RED, TextDecoration.BOLD));
            }
        } else if (outcome == GuardOutcome.GUARD_BLOCK && attacker instanceof Player attackerPlayer) {
            ItemStack held = attackerPlayer.getInventory().getItemInMainHand();
            CustomItem ci = resolveCustomItem(held);
            MeleeWeaponArchetype arch = ci != null ? MeleeWeaponArchetype.fromWeaponType(ci.getWeaponType()) : null;
            fpAnimator.playSequence(attackerPlayer, "recoil", MeleeFirstPersonAnimator.RECOIL_FRAME_COUNT, 300L, false);
            displayAnimator.applyPoseQuat(attackerPlayer, held, 0.44f, 1.46f, 0.24f,
                    MeleeDisplayAnimator.slash3PQuat(20f, -30f, -24f, 0f), 2,
                    scaleFor(arch != null ? arch : MeleeWeaponArchetype.SWORD));
            activeRecoils.put(attackerId, new RecoilState(6, "recoil", MeleeFirstPersonAnimator.RECOIL_FRAME_COUNT));
        }
    }

    private void ensureNoBobAttackSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) return;
        if (attr.getModifier(noBobAttackSpeedKey) == null) {
            attr.addTransientModifier(new AttributeModifier(
                    noBobAttackSpeedKey, 100.0, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void removeNoBobAttackSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(noBobAttackSpeedKey);
        if (existing != null) {
            attr.removeModifier(existing);
        }
    }

    private CustomItem resolveCustomItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        String id = pdcUtil.getItemId(item);
        if (id == null || id.isBlank()) return null;
        return itemManager.getCustomItem(id);
    }

    private Color resolveWeaponVfxColor(CustomItem item, MeleeWeaponArchetype archetype) {
        Map<StatType, Double> stats = item.getBaseStats();
        if (stats != null) {
            if (stats.getOrDefault(StatType.ABYSS_DAMAGE, 0.0) > 0.0
                    || stats.getOrDefault(StatType.PHYS_TO_ABYSS_CONVERSION, 0.0) > 0.0) {
                return Color.fromRGB(185, 82, 255);
            }
            if (stats.getOrDefault(StatType.FIRE_DAMAGE, 0.0) > 0.0
                    || stats.getOrDefault(StatType.PHYS_TO_FIRE_CONVERSION, 0.0) > 0.0) {
                return Color.fromRGB(255, 118, 36);
            }
            if (stats.getOrDefault(StatType.ICE_DAMAGE, 0.0) > 0.0
                    || stats.getOrDefault(StatType.PHYS_TO_ICE_CONVERSION, 0.0) > 0.0) {
                return Color.fromRGB(130, 232, 255);
            }
            if (stats.getOrDefault(StatType.LIGHTNING_DAMAGE, 0.0) > 0.0
                    || stats.getOrDefault(StatType.PHYS_TO_LIGHTNING_CONVERSION, 0.0) > 0.0) {
                return Color.fromRGB(255, 238, 100);
            }
            if (stats.getOrDefault(StatType.HOLY_DAMAGE, 0.0) > 0.0) {
                return Color.fromRGB(255, 242, 175);
            }
        }
        return switch (archetype) {
            case SCYTHE -> Color.fromRGB(190, 95, 255);
            case DAGGER -> Color.fromRGB(155, 250, 225);
            case SPEAR -> Color.fromRGB(155, 230, 255);
            default -> Color.fromRGB(255, 232, 150);
        };
    }

    private float scaleFor(MeleeWeaponArchetype archetype) {
        return switch (archetype) {
            case GREATSWORD -> 1.35f;
            case SPEAR, SCYTHE -> 1.25f;
            case AXE, HAMMER -> 1.18f;
            case DAGGER -> 0.85f;
            case SWORD -> 1.05f;
        };
    }

    private static final class ActiveSwing {
        final UUID playerId;
        final ItemStack weaponItem;
        final MeleeWeaponArchetype archetype;
        final int stepIndex;
        final ComboStep step;
        final Color vfxColor;
        final int windupTicks;
        final int activeTicks;
        final int recoveryTicks;
        final Set<UUID> hitEntities = new HashSet<>();
        int tick = 0;
        int hitstopTicks = 0;
        boolean hitstopTriggered = false;

        ActiveSwing(UUID playerId, ItemStack weaponItem, MeleeWeaponArchetype archetype, int stepIndex,
                    ComboStep step, Color vfxColor, int windupTicks, int activeTicks, int recoveryTicks) {
            this.playerId = playerId;
            this.weaponItem = weaponItem;
            this.archetype = archetype;
            this.stepIndex = stepIndex;
            this.step = step;
            this.vfxColor = vfxColor;
            this.windupTicks = windupTicks;
            this.activeTicks = activeTicks;
            this.recoveryTicks = recoveryTicks;
        }
    }

    private record ComboProgress(MeleeWeaponArchetype archetype, int lastStepIndex, long lastFinishedMillis) {
    }

    private static final class RecoilState {
        final int totalTicks;
        final String fpSequence;
        final int fpFrameCount;
        int remainingTicks;

        RecoilState(int totalTicks, String fpSequence, int fpFrameCount) {
            this.totalTicks = totalTicks;
            this.remainingTicks = totalTicks;
            this.fpSequence = fpSequence;
            this.fpFrameCount = fpFrameCount;
        }
    }
}
