package com.ruskserver.deepwither_V2.modules.combat.damage;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.DamagePhase;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.ItemAbilityPhase;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.MartyrdomPhase;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.SpecialEffectPhase;
import com.ruskserver.deepwither_V2.modules.skill.definitions.MartyrdomSkill;
import com.ruskserver.deepwither_V2.modules.combat.feedback.DamageFeedbackService;
import com.ruskserver.deepwither_V2.modules.combat.feedback.DamageIndicatorService;
import com.ruskserver.deepwither_V2.modules.combat.feedback.CriticalHitFeedbackService;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.combat.stagger.BossStaggerService;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.modifier.SpecialEffectService;
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
    private final StatManager statManager;
    private final CustomMobManager customMobManager;
    private final MobRegionConfig regionConfig;
    private final TraderService traderService;
    private final DamageFeedbackService feedbackService;
    private final DamageIndicatorService indicatorService;
    private final CriticalHitFeedbackService criticalHitFeedbackService;
    private final BossStaggerService staggerService;
    private final PartyManager partyManager;
    private final NamespacedKey corpseKey;
    private final List<DamagePhase> pipeline = new ArrayList<>();

    // 各エンティティの次回の攻撃可能時刻を管理 (無敵時間システム)
    private static final long DEFAULT_IFRAME_MILLIS = 500L;
    private final Map<DamageIFrameKey, Long> nextDamageTimeMap = new ConcurrentHashMap<>();

    @Inject
    public DamagePipelineManager(VirtualHealthManager healthManager, ManaManager manaManager, StatManager statManager,
                                 ItemManager itemManager, ItemPDCUtil pdcUtil, SpecialEffectService specialEffectService,
                                 CustomMobManager customMobManager, MobRegionConfig regionConfig,
                                 TraderService traderService, DamageFeedbackService feedbackService,
                                 DamageIndicatorService indicatorService,
                                 CriticalHitFeedbackService criticalHitFeedbackService,
                                 BossStaggerService staggerService,
                                 PartyManager partyManager, MartyrdomSkill martyrdomSkill,
                                 org.bukkit.plugin.java.JavaPlugin plugin) {
        this.healthManager = healthManager;
        this.statManager = statManager;
        this.customMobManager = customMobManager;
        this.regionConfig = regionConfig;
        this.traderService = traderService;
        this.feedbackService = feedbackService;
        this.indicatorService = indicatorService;
        this.criticalHitFeedbackService = criticalHitFeedbackService;
        this.staggerService = staggerService;
        this.partyManager = partyManager;
        this.corpseKey = new NamespacedKey(plugin, RevivalManager.CORPSE_TAG);

        // パイプラインのフェーズを順番に登録する
        // 1. 基礎ダメージの設定
        pipeline.add(new DamagePhase.Base(statManager));
        pipeline.add(new DamagePhase.PotionEffects());
        // 2. クリティカルの判定
        pipeline.add(new DamagePhase.Critical(statManager));
        // 3. 防御力による軽減
        pipeline.add(new DamagePhase.Defense(statManager));
        // 4. 装備アイテムの固有能力（パッシブ）の適用
        pipeline.add(new ItemAbilityPhase(itemManager, pdcUtil));
        // 5. 属性別ダメージ補正（火・氷などのパッシブ効果）
        pipeline.add(new DamagePhase.ElementModifier(statManager));
        pipeline.add(new SpecialEffectPhase(specialEffectService, healthManager, manaManager));
        // 6. スキル「殉教」等の味方被ダメージ肩代わり効果の適用
        pipeline.add(new MartyrdomPhase(martyrdomSkill, healthManager));
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

        // 最終ダメージを仮想HPから減算し、フィードバックを再生
        if (context.getDamage() > 0) {
            customMobManager.recordDamage(defender, attacker);
            healthManager.damage(defender, context.getDamage());
            indicatorService.show(context);
            criticalHitFeedbackService.show(context);
            staggerService.recordResolvedDamage(context);
            
            // 無敵時間を設定 (500ms = 0.5秒)
            applyIFrame(iframeKey, now, DEFAULT_IFRAME_MILLIS);
            
            // 攻撃の方向（Yaw）を計算して視界の揺れに反映
            float yaw = 0f;
            if (attacker != null) {
                org.bukkit.util.Vector dir = attacker.getLocation().toVector().subtract(defender.getLocation().toVector());
                yaw = (float) (Math.atan2(dir.getZ(), dir.getX()) * 180 / Math.PI) - 90f;
            }
            feedbackService.playHurtFeedback(defender, yaw);
        }
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
            healthManager.damage(defender, finalDamage);
            feedbackService.playHurtFeedback(defender);

            // 環境ダメージ後も無敵時間を設定
            applyIFrame(iframeKey, now, DEFAULT_IFRAME_MILLIS);
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

        if (context.getDamage() > 0) {
            customMobManager.recordDamage(defender, attacker);
            healthManager.damage(defender, context.getDamage());
            indicatorService.show(context);
            criticalHitFeedbackService.show(context);
            staggerService.recordResolvedDamage(context);

            // 無敵時間を設定
            applyIFrame(iframeKey, now, iframeMillis);

            // 攻撃の方向（Yaw）を計算して視界の揺れに反映
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
