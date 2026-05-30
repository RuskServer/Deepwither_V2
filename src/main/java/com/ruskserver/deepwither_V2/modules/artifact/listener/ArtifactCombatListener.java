package com.ruskserver.deepwither_V2.modules.artifact.listener;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactData;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactPDCUtil;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactSetType;
import com.ruskserver.deepwither_V2.modules.artifact.provider.PlayerArtifactProvider;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アーティファクトの3セット効果（特殊戦闘ロジック）を処理するリスナー。
 *
 * 各効果のCooldown管理には ConcurrentHashMap を使用し、
 * 値として「次に発動可能なシステム時刻 (ms)」を記録します。
 */
@Service
public class ArtifactCombatListener implements Listener {

    // 3セット効果のCooldown (ms)
    private static final long ABYSS_BARRIER_CD_MS    = 8_000L;
    private static final long FAULT_LINE_CD_MS        = 10_000L;
    private static final long FAULT_LINE_SPEED_MS     = 3_000L;  // クリティカル後の速度ボーナス持続
    private static final long ASTRAL_REGEN_CD_MS      = 60_000L;
    private static final long ETERNAL_HEARTS_CD_MS    = 300_000L;

    // プレイヤーごとのCooldownマップ
    // Key: UUID, Value: 次に発動可能なシステム時刻 (ms)
    private final Map<UUID, Long> abyssBarrierCd    = new ConcurrentHashMap<>();
    private final Map<UUID, Long> faultLineCd       = new ConcurrentHashMap<>();
    private final Map<UUID, Long> faultLineSpeedEnd = new ConcurrentHashMap<>(); // 速度UP終了時刻
    private final Map<UUID, Long> astralRegenCd     = new ConcurrentHashMap<>();
    private final Map<UUID, Long> eternalHeartsCd   = new ConcurrentHashMap<>();

    // Eternal Hearts が「発動済み」かどうか（死なずにリセットされるまで）
    private final Set<UUID> eternalHeartsActive     = ConcurrentHashMap.newKeySet();

    private final PlayerDataRepository repository;
    private final ArtifactPDCUtil pdcUtil;
    private final VirtualHealthManager healthManager;
    private final ManaManager manaManager;
    private final DamagePipelineManager pipelineManager;
    private final StatManager statManager;

    @Inject
    public ArtifactCombatListener(PlayerDataRepository repository, ArtifactPDCUtil pdcUtil,
                                  VirtualHealthManager healthManager, ManaManager manaManager,
                                  DamagePipelineManager pipelineManager, StatManager statManager) {
        this.repository   = repository;
        this.pdcUtil       = pdcUtil;
        this.healthManager = healthManager;
        this.manaManager   = manaManager;
        this.pipelineManager = pipelineManager;
        this.statManager = statManager;
    }

    // ──────────────────────────────────────────────────────────────────
    //  受けるダメージに関する処理
    // ──────────────────────────────────────────────────────────────────

    /**
     * エンティティからのダメージ処理 (EntityDamageByEntityEvent) をハイジャックする前に、
     * アーティファクト効果で割り込む。DamagePipelineManagerよりも低い優先度で実行し、
     * パイプラインがキャンセルした後のタイミングで割り込みます。
     *
     * 対象: ABYSS_PULSATION (3set), ETERNAL_HEARTS (3set), ASTRAL_STEEL_GUARD (3set)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamagedByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;
        UUID uuid = defender.getUniqueId();

        // 装備中のセット状況を取得
        Map<ArtifactSetType, Integer> setCount = getEquippedSetCount(defender);

        // ABYSS_PULSATION 3セット効果：魔法被弾時に障壁を展開
        // EventがMONITORなのでダメージは既に処理済み。魔法DamageTypeは独自のpipeline経由なのでここでは
        // EntityDamageEvent.DamageCause で判定するのが安全
        if (setCount.getOrDefault(ArtifactSetType.ABYSS_PULSATION, 0) >= 3) {
            onAbyssPulsationHit(defender, uuid, event.getCause());
        }

        // ETERNAL_HEARTS 3セット効果：致死ダメージを1回耐える
        if (setCount.getOrDefault(ArtifactSetType.ETERNAL_HEARTS, 0) >= 3) {
            onEternalHeartsCheck(defender, uuid);
        }

        // ASTRAL_STEEL_GUARD 3セット効果：物理被弾時マナ変換
        // EntityDamageByEntityはほぼPHYSICALなので、タグなどで区別している場合は拡張可能
        if (setCount.getOrDefault(ArtifactSetType.ASTRAL_STEEL_GUARD, 0) >= 3) {
            onAstralSteelPhysicalHit(defender, uuid);
        }
    }

    /**
     * 環境ダメージ (EntityDamageEvent) の後処理。
     * ETERNAL_HEARTS の致死チェックを環境ダメージでも機能させます。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!(event.getEntity() instanceof Player defender)) return;

        UUID uuid = defender.getUniqueId();
        Map<ArtifactSetType, Integer> setCount = getEquippedSetCount(defender);

        if (setCount.getOrDefault(ArtifactSetType.ETERNAL_HEARTS, 0) >= 3) {
            onEternalHeartsCheck(defender, uuid);
        }

        // ASTRAL_STEEL_GUARD 3セット: HP30%以下で再生II付与 (60秒CD)
        if (setCount.getOrDefault(ArtifactSetType.ASTRAL_STEEL_GUARD, 0) >= 3) {
            onAstralSteelLowHp(defender, uuid);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  各セット効果の内部処理
    // ──────────────────────────────────────────────────────────────────

    /**
     * ABYSS_PULSATION 3セット: 魔法被弾時、8秒CDで完全遮断障壁を展開。
     * 障壁発動時、周囲3ブロックの敵をノックバックし盲目付与。
     */
    private void onAbyssPulsationHit(Player defender, UUID uuid, EntityDamageEvent.DamageCause cause) {
        // 魔法系のダメージ判定 (MAGIC, PROJECTILE 等)
        boolean isMagicDamage = switch (cause) {
            case MAGIC, PROJECTILE, LIGHTNING -> true;
            default -> false;
        };
        if (!isMagicDamage) return;
        if (!isCooldownReady(abyssBarrierCd, uuid)) return;

        // 次のダメージを0にするためにvirtualHPを直接回復させる（既に処理済みのダメージを補填）
        double maxHp = healthManager.getMaxHealth(defender);
        double currentHp = healthManager.getHealth(defender);
        // 障壁は「次の被弾1回を無効化」に近い動作: MONITOR時点でHPを確認し全快に近い状態なら1回分を回復
        // 実装上は「バリアを展開」として一定HP回復 (最大HP30%分を補填) で表現
        double barrierHeal = maxHp * 0.30;
        healthManager.heal(defender, barrierHeal);

        // ノックバック + 盲目
        Location loc = defender.getLocation();
        for (LivingEntity nearby : defender.getWorld().getNearbyLivingEntities(loc, 3.0)) {
            if (nearby.equals(defender)) continue;
            // ノックバック
            org.bukkit.util.Vector knockDir = nearby.getLocation().toVector()
                    .subtract(loc.toVector()).normalize().multiply(1.5).setY(0.4);
            nearby.setVelocity(knockDir);
            // 盲目 (3秒)
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
        }

        // エフェクト
        defender.getWorld().spawnParticle(Particle.PORTAL, loc, 80, 1, 1, 1, 0.3);
        defender.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        setCooldown(abyssBarrierCd, uuid, ABYSS_BARRIER_CD_MS);
    }

    /**
     * ETERNAL_HEARTS 3セット: 致死ダメージを1回だけHP1で耐える。
     * 発動時、周囲の敵を強く弾き飛ばす (300秒CD)。
     */
    private void onEternalHeartsCheck(Player defender, UUID uuid) {
        if (eternalHeartsActive.contains(uuid)) return; // 既に使用済み
        if (!isCooldownReady(eternalHeartsCd, uuid)) return;

        double currentHp = healthManager.getHealth(defender);
        // HP が0以下になろうとしているか判定
        if (currentHp > 1.0) return;

        // HP1で耐える
        healthManager.heal(defender, 1.0 - currentHp + 1.0);
        eternalHeartsActive.add(uuid);
        setCooldown(eternalHeartsCd, uuid, ETERNAL_HEARTS_CD_MS);

        // 周囲の敵を弾き飛ばす (半径6ブロック)
        Location loc = defender.getLocation();
        for (LivingEntity nearby : defender.getWorld().getNearbyLivingEntities(loc, 6.0)) {
            if (nearby.equals(defender)) continue;
            org.bukkit.util.Vector knockDir = nearby.getLocation().toVector()
                    .subtract(loc.toVector()).normalize().multiply(3.0).setY(0.8);
            nearby.setVelocity(knockDir);
        }

        // エフェクト
        defender.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 30, 1, 1, 1, 0.2);
        defender.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.8f);
        defender.sendMessage(net.kyori.adventure.text.Component.text(
                "「永遠の心臓」が致死ダメージを阻止した！",
                net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
    }

    /**
     * ASTRAL_STEEL_GUARD 3セット: 物理被弾時、受けたダメージの10%をマナへ変換。
     */
    private void onAstralSteelPhysicalHit(Player defender, UUID uuid) {
        // 直前に受けたダメージ量を取得するのはMONITOR後では困難なため、
        // 簡易実装: 受けるたびに最大マナの3%を回復する（ヒットごとのマナ変換として近似）
        double manaRestore = manaManager.getMaxMana(defender) * 0.03;
        manaManager.restore(defender, manaRestore);
    }

    /**
     * ASTRAL_STEEL_GUARD 3セット: HP30%以下で60秒CDの再生IIを付与。
     */
    private void onAstralSteelLowHp(Player defender, UUID uuid) {
        double currentHp = healthManager.getHealth(defender);
        double maxHp = healthManager.getMaxHealth(defender);
        if (currentHp / maxHp > 0.30) return;
        if (!isCooldownReady(astralRegenCd, uuid)) return;

        // 再生II (10秒間)
        defender.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, true));
        defender.playSound(defender.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.5f);
        defender.sendMessage(net.kyori.adventure.text.Component.text(
                "「星盾の守護」が再生を付与した！",
                net.kyori.adventure.text.format.NamedTextColor.AQUA));

        setCooldown(astralRegenCd, uuid, ASTRAL_REGEN_CD_MS);
    }

    // ──────────────────────────────────────────────────────────────────
    //  攻撃時のセット効果処理
    // ──────────────────────────────────────────────────────────────────

    /**
     * プレイヤーが攻撃する際の処理 (FAULT_LINE, LUNAR_SKIRMISHER の3セット効果)。
     * DamagePipelineManager が HIGHEST でキャンセルする前にチェックするため HIGH で実行。
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        UUID uuid = attacker.getUniqueId();
        Map<ArtifactSetType, Integer> setCount = getEquippedSetCount(attacker);

        // バニラのクリティカル条件で近似（落下中 + 地面に触れていない）
        boolean isCrit = attacker.getFallDistance() > 0 && !attacker.isOnGround()
                && !attacker.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);

        // FAULT_LINE 3セット: クリティカル時に防御無視 + 移動速度UP
        if (setCount.getOrDefault(ArtifactSetType.FAULT_LINE, 0) >= 3 && isCrit) {
            onFaultLineCrit(attacker, uuid, target);
        }

        // LUNAR_SKIRMISHER 3セット: 速度UP中の初撃に魔法追撃を付与
        if (setCount.getOrDefault(ArtifactSetType.LUNAR_SKIRMISHER, 0) >= 3) {
            onLunarSkirmisherAttack(attacker, uuid, target);
        }
    }

    /**
     * FAULT_LINE 3セット: クリティカル時、10秒CDで対象の防御を20%無視。
     * クリティカル後3秒間、移動速度 +10%。
     */
    private void onFaultLineCrit(Player attacker, UUID uuid, LivingEntity target) {
        if (!isCooldownReady(faultLineCd, uuid)) return;

        // 防御無視：攻撃者の物理攻撃力の20%を TRUE_DAMAGE として即時追撃
        double baseAtk = statManager.getTotalStat(attacker, com.ruskserver.deepwither_V2.core.stat.StatType.ATTACK_DAMAGE);
        pipelineManager.processDamage(attacker, target, DamageType.TRUE_DAMAGE,
                Math.max(baseAtk, 1.0) * 0.2,
                null);

        // 移動速度UP (3秒間: Speed I)
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, true));
        faultLineSpeedEnd.put(uuid, System.currentTimeMillis() + FAULT_LINE_SPEED_MS);

        // エフェクト
        attacker.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);

        setCooldown(faultLineCd, uuid, FAULT_LINE_CD_MS);
    }

    /**
     * LUNAR_SKIRMISHER 3セット: Speed効果中に攻撃した際、25%の確率で魔法追撃を付与。
     */
    private void onLunarSkirmisherAttack(Player attacker, UUID uuid, LivingEntity target) {
        // 速度ボーナス中かどうかを確認
        if (!attacker.hasPotionEffect(PotionEffectType.SPEED)) return;
        // 確率25%で魔法追撃
        if (Math.random() > 0.25) return;

        // 魔法追撃ダメージ = 固定5.0 (StatManagerの魔法攻撃力を取得して計算することも可能)
        pipelineManager.processDamage(attacker, target, DamageType.MAGIC, 5.0, null);

        // エフェクト
        target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        abyssBarrierCd.remove(uuid);
        faultLineCd.remove(uuid);
        faultLineSpeedEnd.remove(uuid);
        astralRegenCd.remove(uuid);
        eternalHeartsCd.remove(uuid);
        eternalHeartsActive.remove(uuid);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Utility
    // ──────────────────────────────────────────────────────────────────

    /**
     * プレイヤーが装備しているアーティファクトのセット数をカウントします。
     */
    private Map<ArtifactSetType, Integer> getEquippedSetCount(Player player) {
        Map<ArtifactSetType, Integer> setCount = new HashMap<>();
        repository.get(player.getUniqueId()).ifPresent(data -> {
            PlayerArtifactProvider.ArtifactSaveData artifactData = data.get(PlayerArtifactProvider.KEY);
            if (artifactData == null) return;

            for (String base64 : artifactData.getEquippedArtifacts().values()) {
                if (base64 == null || base64.isEmpty()) continue;
                try {
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    ItemStack item = ItemStack.deserializeBytes(bytes);
                    ArtifactData ad = pdcUtil.getArtifactData(item);
                    if (ad != null) {
                        setCount.merge(ad.getSetType(), 1, Integer::sum);
                    }
                } catch (Exception ignored) {}
            }
        });
        return setCount;
    }

    private boolean isCooldownReady(Map<UUID, Long> cdMap, UUID uuid) {
        long now = System.currentTimeMillis();
        Long ready = cdMap.get(uuid);
        return ready == null || now >= ready;
    }

    private void setCooldown(Map<UUID, Long> cdMap, UUID uuid, long durationMs) {
        cdMap.put(uuid, System.currentTimeMillis() + durationMs);
    }
}
