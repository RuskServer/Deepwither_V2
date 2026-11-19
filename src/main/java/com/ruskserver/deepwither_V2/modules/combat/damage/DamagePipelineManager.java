package com.ruskserver.deepwither_V2.modules.combat.damage;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.combat.CombatStateService;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.DamagePhase;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.ItemAbilityPhase;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.EquipmentSetPhase;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.MartyrdomPhase;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.SpecialEffectPhase;
import com.ruskserver.deepwither_V2.modules.skill.definitions.MartyrdomSkill;
import com.ruskserver.deepwither_V2.modules.combat.feedback.DamageFeedbackService;
import com.ruskserver.deepwither_V2.modules.combat.feedback.DamageIndicatorService;
import com.ruskserver.deepwither_V2.modules.combat.feedback.CriticalHitFeedbackService;
import com.ruskserver.deepwither_V2.modules.combat.health.MagicShieldManager;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.combat.stagger.BossStaggerService;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.modifier.SpecialEffectService;
import com.ruskserver.deepwither_V2.modules.item.durability.EquipmentDurabilityService;
import com.ruskserver.deepwither_V2.modules.item.set.EquipmentSetService;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.mob.region.MobRegionConfig;
import com.ruskserver.deepwither_V2.modules.party.Party;
import com.ruskserver.deepwither_V2.modules.party.PartyManager;
import com.ruskserver.deepwither_V2.modules.revival.RevivalManager;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.modules.trader.service.TraderService;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * バニラのダメージ処理をインターセプトし、パイプラインを実行して仮想HPに反映させるマネージャー。
 */
@Component
public class DamagePipelineManager implements Listener {

    private final VirtualHealthManager healthManager;
    private final MagicShieldManager magicShieldManager;
    private final CombatStateService combatState;
    private final StatManager statManager;
    private final CustomMobManager customMobManager;
    private final MobRegionConfig regionConfig;
    private final TraderService traderService;
    private final DamageFeedbackService feedbackService;
    private final DamageIndicatorService indicatorService;
    private final CriticalHitFeedbackService criticalHitFeedbackService;
    private final BossStaggerService staggerService;
    private final PartyManager partyManager;
    private final EquipmentSetService equipmentSetService;
    private final EquipmentDurabilityService durabilityService;
    private final NamespacedKey corpseKey;
    private final List<DamagePhase> pipeline = new ArrayList<>();
    private final DamageMath.EvasionEntropyTracker evasionEntropyTracker = new DamageMath.EvasionEntropyTracker();

    // 各エンティティの次回の攻撃可能時刻を管理 (無敵時間システム)
    private static final long DEFAULT_IFRAME_MILLIS = 500L;
    private final Map<DamageIFrameKey, Long> nextDamageTimeMap = new ConcurrentHashMap<>();

    @Inject
    public DamagePipelineManager(VirtualHealthManager healthManager, MagicShieldManager magicShieldManager,
                                 ManaManager manaManager, StatManager statManager,
                                 ItemManager itemManager, ItemPDCUtil pdcUtil, SpecialEffectService specialEffectService,
                                 CustomMobManager customMobManager, MobRegionConfig regionConfig,
                                 TraderService traderService, DamageFeedbackService feedbackService,
                                 DamageIndicatorService indicatorService,
                                 CriticalHitFeedbackService criticalHitFeedbackService,
                                 BossStaggerService staggerService,
                                 PartyManager partyManager, MartyrdomSkill martyrdomSkill,
                                 EquipmentSetService equipmentSetService,
                                 EquipmentDurabilityService durabilityService,
                                 org.bukkit.plugin.java.JavaPlugin plugin, CombatStateService combatState) {
        this.combatState = combatState;
        this.healthManager = healthManager;
        this.magicShieldManager = magicShieldManager;
        this.statManager = statManager;
        this.customMobManager = customMobManager;
        this.regionConfig = regionConfig;
        this.traderService = traderService;
        this.feedbackService = feedbackService;
        this.indicatorService = indicatorService;
        this.criticalHitFeedbackService = criticalHitFeedbackService;
        this.staggerService = staggerService;
        this.partyManager = partyManager;
        this.equipmentSetService = equipmentSetService;
        this.durabilityService = durabilityService;
        this.corpseKey = new NamespacedKey(plugin, RevivalManager.CORPSE_TAG);

        // パイプラインのフェーズを PoE / MythicLib 式の順番で登録する
        // 1. 基礎ダメージ（物理 + フラット属性 / 深淵）
        pipeline.add(new DamagePhase.Base(statManager));
        // 2. 属性変換 (物理 -> 雷 -> 氷 -> 火 -> 深淵 / 属性 -> 深淵)
        pipeline.add(new DamagePhase.Conversion(statManager));
        // 3. スケーリング (Increased 加算合算 × More 独立乗算)
        pipeline.add(new DamagePhase.Scaling(statManager));
        // 4. ポーション効果
        pipeline.add(new DamagePhase.PotionEffects());
        // 5. 回避（エントロピー式）・ブロック判定
        pipeline.add(new DamagePhase.Avoidance(statManager, evasionEntropyTracker));
        // 6. クリティカル判定
        pipeline.add(new DamagePhase.Critical(statManager));
        // 7. 防御・耐性・貫通（PoE アーマー式 + 属性/深淵耐性 + ペネトレーション）
        pipeline.add(new DamagePhase.Defense(statManager));
        // 8. 装備アイテム固有能力・特殊効果・セット効果
        pipeline.add(new ItemAbilityPhase(itemManager, pdcUtil));
        pipeline.add(new SpecialEffectPhase(specialEffectService, healthManager, manaManager));
        pipeline.add(new EquipmentSetPhase(equipmentSetService));
        // 9. スキル「殉教」等の味方被ダメージ肩代わり効果
        pipeline.add(new MartyrdomPhase(martyrdomSkill, healthManager, combatState));
    }

    /**
     * エンティティ同士の戦闘ダメージ処理。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity defender)) return;

        if (isProtectedTrader(defender)) {
            cancelDamage(event);
            return;
        }

        LivingEntity attacker = resolveAttacker(event);
        if (attacker == null) return;

        if (isBlockedPvp(attacker, defender)) {
            cancelDamage(event);
            return;
        }

        if (isSameParty(attacker, defender)) {
            cancelDamage(event);
            return;
        }

        // 無敵時間（i-frame）のチェック
        long now = System.currentTimeMillis();
        DamageIFrameKey iframeKey = iframeKey(attacker, defender, "vanilla:" + event.getCause().name());
        if (isInvulnerable(iframeKey, now)) {
            event.setCancelled(true);
            return;
        }

        cancelDamage(event);

        // ダメージタイプの判定（実体の矢の場合はRANGED、それ以外はPHYSICAL）
        DamageType type = event.getDamager() instanceof org.bukkit.entity.Arrow ? DamageType.RANGED : DamageType.PHYSICAL;

        // パイプラインを通すためのコンテキストを生成
        DamageContext context = new DamageContext(attacker, defender, type, 0.0);
        if (event.getDamager() instanceof Projectile projectile) {
            context.setImpactLocation(projectile.getLocation());
        }

        // パイプライン処理を実行
        for (DamagePhase phase : pipeline) {
            phase.process(context);
        }

        // 攻撃側がカスタムモブの場合、レベルに応じたダメージ補正を適用
        CustomMob attackerMob = customMobManager.getCustomMob(attacker);
        if (attackerMob != null) {
            double multiplier = customMobManager.getDamageMultiplier();
            double factor = 1.0 + (attackerMob.getLevel() - 1) * multiplier;
            context.setDamage(context.getDamage() * factor);
        }
        staggerService.applyStaggeredDamageMultiplier(context);

        // 最終ダメージをマジックシールド（深淵/Trueは貫通）および仮想HPへ反映
        applyResolvedDamage(context, attacker, defender, iframeKey, now, DEFAULT_IFRAME_MILLIS);
    }

    /**
     * 環境ダメージ（落下、炎、毒、爆発など）の処理。
     * エンティティからの直接攻撃以外の全てのダメージがここに入ります。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        // EntityDamageByEntityEventは上で処理しているので弾く
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!(event.getEntity() instanceof LivingEntity defender)) return;

        if (isProtectedTrader(defender)) {
            cancelDamage(event);
            return;
        }

        // 無敵時間（i-frame）のチェック
        long now = System.currentTimeMillis();
        DamageIFrameKey iframeKey = iframeKey(null, defender, "environment:" + event.getCause().name());
        if (isInvulnerable(iframeKey, now)) {
            event.setCancelled(true);
            return;
        }

        // バニラの計算をキャンセル
        double vanillaDamage = event.getDamage();
        event.setDamage(0);
        event.setCancelled(true);

        if (vanillaDamage <= 0) return;

        // バニラダメージを仮想HPの割合ダメージに変換
        // プレイヤーは常に20.0基準、MobはバニラのMAX_HEALTH属性を基準とする
        double baseHealth = 20.0;
        if (!(defender instanceof Player)) {
            AttributeInstance attr = defender.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) baseHealth = attr.getValue();
        }
        double ratio = vanillaDamage / baseHealth;
        double maxHp = healthManager.getMaxHealth(defender);
        double finalDamage = maxHp * ratio;

        if (finalDamage > 0) {
            boolean bypassesEs = switch (event.getCause()) {
                case POISON, WITHER, VOID, STARVATION -> true;
                default -> false;
            };
            double hpDamage;
            if (bypassesEs) {
                magicShieldManager.recordDamageTaken(defender);
                hpDamage = finalDamage;
            } else {
                hpDamage = magicShieldManager.absorbDamage(defender, finalDamage);
            }
            if (hpDamage > 0) {
                healthManager.damage(defender, hpDamage);
            }
            feedbackService.playHurtFeedback(defender);

            // 環境ダメージ後も無敵時間を設定
            applyIFrame(iframeKey, now, DEFAULT_IFRAME_MILLIS);
        }
    }

    private void recordCombat(DamageContext context) {
        LivingEntity defender = context.getDefender();
        if (context.getType() != DamageType.ENVIRONMENTAL && !defender.isDead()
                && healthManager.getHealth(defender) > 0) {
            combatState.recordAttack(context.getAttacker(), defender);
        }
    }

    private LivingEntity resolveAttacker(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private boolean isProtectedTrader(LivingEntity entity) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            return false;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
        return npc != null && traderService.getTrader(npc.getName()) != null;
    }

    private boolean isBlockedPvp(LivingEntity attacker, LivingEntity defender) {
        if (!(attacker instanceof Player) || !(defender instanceof Player)) {
            return false;
        }
        return regionConfig.isInPvpDisabledRegion(attacker.getLocation())
                || regionConfig.isInPvpDisabledRegion(defender.getLocation());
    }

    private boolean isSameParty(LivingEntity attacker, LivingEntity defender) {
        if (!(attacker instanceof Player playerA) || !(defender instanceof Player playerD)) {
            return false;
        }
        Party party = partyManager.getParty(playerA);
        return party != null && party.isMember(playerD.getUniqueId());
    }

    private void cancelDamage(EntityDamageEvent event) {
        event.setDamage(0);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        nextDamageTimeMap.keySet().removeIf(key ->
                key.defenderId().equals(playerId) || playerId.equals(key.attackerId()));
        evasionEntropyTracker.remove(playerId);
    }

    /**
     * 外部システム（杖の魔法弾など）から直接ダメージパイプラインにダメージ処理を流し込むための公開API。
     */
    public void processDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage, java.util.Set<String> tags) {
        processDamage(attacker, defender, type, initialDamage, tags, 1.0,
                "direct:" + type.name(), DEFAULT_IFRAME_MILLIS);
    }

    public void processDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage,
                              java.util.Set<String> tags, String sourceId, long iframeMillis) {
        processDamage(attacker, defender, type, initialDamage, tags, 1.0, sourceId, iframeMillis);
    }

    public void processDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage,
                              java.util.Set<String> tags, String sourceId, long iframeMillis,
                              Location impactLocation) {
        processDamage(attacker, defender, type, initialDamage, tags, 1.0, sourceId, iframeMillis, impactLocation);
    }

    /**
     * 攻撃力または魔法攻撃力に対する倍率でスキルダメージを流し込むためのAPI。
     */
    public void processScaledDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double coefficient, java.util.Set<String> tags) {
        processScaledDamage(attacker, defender, type, coefficient, tags,
                "skill:" + type.name(), DEFAULT_IFRAME_MILLIS);
    }

    public void processScaledDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double coefficient,
                                    java.util.Set<String> tags, String sourceId, long iframeMillis) {
        processScaledDamage(attacker, defender, type, coefficient, tags, sourceId, iframeMillis, null);
    }

    public void processScaledDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double coefficient,
                                    java.util.Set<String> tags, String sourceId, long iframeMillis,
                                    Location impactLocation) {
        processScaledDamage(attacker, defender, type, coefficient, tags, sourceId, iframeMillis,
                impactLocation, 1.0);
    }

    public void processScaledDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double coefficient,
                                    java.util.Set<String> tags, String sourceId, long iframeMillis,
                                    Location impactLocation, double staggerMultiplier) {
        if (attacker == null) {
            processDamage(null, defender, type, 0.0, tags, 1.0, sourceId, iframeMillis,
                    impactLocation, staggerMultiplier);
            return;
        }

        StatType statType;
        if (type == DamageType.MAGIC) {
            statType = StatType.MAGIC_DAMAGE;
        } else if (type == DamageType.RANGED) {
            statType = StatType.RANGED_DAMAGE;
        } else {
            statType = StatType.ATTACK_DAMAGE;
        }
        double baseDamage = statManager.getTotalStat(attacker, statType);
        double initialDamage = (baseDamage > 0 ? baseDamage : 1.0) * coefficient;
        processDamage(attacker, defender, type, initialDamage, tags, 1.0, sourceId, iframeMillis,
                impactLocation, staggerMultiplier);
    }

    /**
     * 距離倍率指定可能な processDamage のオーバーロード。
     */
    public void processDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage, java.util.Set<String> tags, double distanceMultiplier) {
        processDamage(attacker, defender, type, initialDamage, tags, distanceMultiplier,
                "direct:" + type.name(), DEFAULT_IFRAME_MILLIS);
    }

    public void processDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage,
                              java.util.Set<String> tags, double distanceMultiplier,
                              String sourceId, long iframeMillis) {
        processDamage(attacker, defender, type, initialDamage, tags, distanceMultiplier,
                sourceId, iframeMillis, null);
    }

    public void processDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage,
                              java.util.Set<String> tags, double distanceMultiplier,
                              String sourceId, long iframeMillis, Location impactLocation) {
        processDamage(attacker, defender, type, initialDamage, tags, distanceMultiplier,
                sourceId, iframeMillis, impactLocation, 1.0);
    }

    public void processDamage(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage,
                              java.util.Set<String> tags, double distanceMultiplier,
                              String sourceId, long iframeMillis, Location impactLocation,
                              double staggerMultiplier) {
        if (isProtectedTrader(defender) || isBlockedPvp(attacker, defender) || isSameParty(attacker, defender)) {
            return;
        }

        // 死体（ダウン中のマネキン）はダメージを受けない
        if (defender.getPersistentDataContainer().has(corpseKey, PersistentDataType.BYTE)) {
            return;
        }

        // スペクテイターはダメージを受けない
        if (defender instanceof Player p && p.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        // 無敵時間（i-frame）のチェック
        long now = System.currentTimeMillis();
        DamageIFrameKey iframeKey = iframeKey(attacker, defender, sourceId);
        if (isInvulnerable(iframeKey, now)) return;

        DamageContext context = new DamageContext(attacker, defender, type, initialDamage);
        context.setDistanceMultiplier(distanceMultiplier);
        context.setImpactLocation(impactLocation);
        context.setStaggerMultiplier(staggerMultiplier);
        if (tags != null) {
            context.addTags(tags);
        }
        for (DamagePhase phase : pipeline) {
            phase.process(context);
        }

        // 攻撃側がカスタムモブの場合、レベルに応じたダメージ補正を適用
        CustomMob attackerMob = customMobManager.getCustomMob(attacker);
        if (attackerMob != null) {
            double multiplier = customMobManager.getDamageMultiplier();
            double factor = 1.0 + (attackerMob.getLevel() - 1) * multiplier;
            context.setDamage(context.getDamage() * factor);
        }
        staggerService.applyStaggeredDamageMultiplier(context);

        applyResolvedDamage(context, attacker, defender, iframeKey, now, iframeMillis);
    }

    private void applyResolvedDamage(DamageContext context, LivingEntity attacker, LivingEntity defender,
                                     DamageIFrameKey iframeKey, long now, long iframeMillis) {
        if (context.isEvaded()) {
            indicatorService.show(context);
            feedbackService.playEvadeFeedback(defender);
            return;
        }
        if (context.getDamage() <= 0) {
            return;
        }

        recordCombat(context);
        equipmentSetService.processResolvedAttackerEffects(context);
        customMobManager.recordDamage(defender, attacker);

        double esBefore = magicShieldManager.getMagicShield(defender);
        double shieldable = context.getShieldableDamage();
        double bypass = context.getMagicShieldBypassDamage();
        double remainingAfterEs = shieldable > 0.0
                ? magicShieldManager.absorbDamage(defender, shieldable)
                : 0.0;
        double esAbsorbed = Math.max(0.0, shieldable - remainingAfterEs);
        context.setMagicShieldAbsorbed(esAbsorbed);
        boolean esBroken = esBefore > 0.0 && esAbsorbed > 0.0
                && magicShieldManager.getMagicShield(defender) <= 0.0;
        if (bypass > 0.0) {
            magicShieldManager.recordDamageTaken(defender);
        }

        double hpDamage = remainingAfterEs + bypass;
        double healthBefore = defender instanceof Player ? healthManager.getHealth(defender) : -1.0;
        if (hpDamage > 0.0) {
            healthManager.damage(defender, hpDamage);
        }
        if (defender instanceof Player player && healthManager.getHealth(defender) < healthBefore) {
            durabilityService.damageArmor(player);
        }

        indicatorService.show(context);
        criticalHitFeedbackService.show(context);
        staggerService.recordResolvedDamage(context);

        // 無敵時間を設定
        applyIFrame(iframeKey, now, iframeMillis);

        if (context.isBlocked()) {
            feedbackService.playBlockFeedback(defender);
        }
        if (esAbsorbed > 0.0) {
            feedbackService.playMagicShieldHitFeedback(defender, esBroken);
        }
        if (bypass > 0.0 && esBefore > 0.0) {
            feedbackService.playAbyssBypassFeedback(defender);
        }

        // HP に実ダメージが入ったときのみ赤点滅＋視界揺れ（ES全吸収時はシアン共鳴のみ）
        if (hpDamage > 0.0) {
            float yaw = 0f;
            if (attacker != null) {
                org.bukkit.util.Vector dir = attacker.getLocation().toVector().subtract(defender.getLocation().toVector());
                yaw = (float) (Math.atan2(dir.getZ(), dir.getX()) * 180 / Math.PI) - 90f;
            }
            feedbackService.playHurtFeedback(defender, yaw);
        }
    }

    private DamageIFrameKey iframeKey(LivingEntity attacker, LivingEntity defender, String sourceId) {
        UUID attackerId = attacker == null ? null : attacker.getUniqueId();
        String normalizedSource = sourceId == null || sourceId.isBlank() ? "unknown" : sourceId;
        return new DamageIFrameKey(defender.getUniqueId(), attackerId, normalizedSource);
    }

    private boolean isInvulnerable(DamageIFrameKey key, long now) {
        Long expiresAt = nextDamageTimeMap.get(key);
        if (expiresAt == null) return false;
        if (expiresAt > now) return true;
        nextDamageTimeMap.remove(key, expiresAt);
        return false;
    }

    private void applyIFrame(DamageIFrameKey key, long now, long iframeMillis) {
        if (iframeMillis <= 0L) {
            nextDamageTimeMap.remove(key);
            return;
        }
        nextDamageTimeMap.put(key, now + iframeMillis);
    }

    private record DamageIFrameKey(UUID defenderId, UUID attackerId, String sourceId) {
    }
}
