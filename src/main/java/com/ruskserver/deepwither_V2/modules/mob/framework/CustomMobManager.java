package com.ruskserver.deepwither_V2.modules.mob.framework;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.party.Party;
import com.ruskserver.deepwither_V2.modules.party.PartyManager;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * カスタムモブのライフサイクルとイベント配送を管理するサービス。
 * <p>
 * モブ定義クラスは {@link #registerMob(String, EntityType, Supplier)} で登録するか、
 * {@code @Component} を付けたモブ定義クラスのコンストラクタ内で自己登録することができます。
 */
@Service
public class CustomMobManager implements Listener, Startable, Stoppable {

    private final JavaPlugin plugin;
    private final VirtualHealthManager healthManager;
    private final PlayerManager playerManager;
    private final PartyManager partyManager;
    private final Logger log;

    /** mob-id → (EntityType, Supplierファクトリ) のレジストリ */
    private final Map<String, EntityType> mobEntityTypes = new ConcurrentHashMap<>();
    private final Map<String, Supplier<CustomMob>> mobFactories = new ConcurrentHashMap<>();

    /** 現在アクティブなモブ: UUID → CustomMobインスタンス */
    private final Map<UUID, CustomMob> activeMobs = new ConcurrentHashMap<>();

    /** カスタムモブUUID → 最後にダメージを与えたプレイヤーUUID */
    private final Map<UUID, UUID> lastPlayerDamagers = new ConcurrentHashMap<>();

    /** 装備アイテムキャッシュ: itemId → クローン元ItemStack */
    private final Map<String, ItemStack> itemCache = new ConcurrentHashMap<>();

    /** エンティティへのモブIDタグに使用するキー */
    private final NamespacedKey mobIdKey;

    private double healthMultiplier = 0.03;
    private double damageMultiplier = 0.01;
    private String nameFormat = "&b[Lv.{level}] &r{name} &c{hp}/{max_hp}♥";

    @Inject
    public CustomMobManager(JavaPlugin plugin, VirtualHealthManager healthManager, PlayerManager playerManager,
                            PartyManager partyManager) {
        this.plugin = plugin;
        this.healthManager = healthManager;
        this.playerManager = playerManager;
        this.partyManager = partyManager;
        this.log = plugin.getLogger();
        this.mobIdKey = new NamespacedKey(plugin, "custom_mob_id");
        loadLevelingConfig();
    }

    private void loadLevelingConfig() {
        var config = plugin.getConfig();
        var section = config.getConfigurationSection("mob-leveling");
        if (section != null) {
            this.healthMultiplier = section.getDouble("health_multiplier", 0.03);
            this.damageMultiplier = section.getDouble("damage_multiplier", 0.01);
            this.nameFormat = section.getString("name_format", "&b[Lv.{level}] &r{name} &c{hp}/{max_hp}♥");
        }
    }

    public double getHealthMultiplier() { return healthMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public String getNameFormat() { return nameFormat; }

    @Override
    public void start() {
        // Tick タスクの開始 (毎tick全アクティブモブを更新)
        plugin.getServer().getScheduler().runTaskTimer(plugin, () ->
                activeMobs.values().forEach(CustomMob::tick), 1L, 1L);

        log.info("[CustomMobManager] 起動しました。登録済みモブ種: " + mobFactories.size() + " 種");
    }

    @Override
    public void stop() {
        activeMobs.clear();
        lastPlayerDamagers.clear();
        itemCache.clear();
        log.info("[CustomMobManager] 停止しました。");
    }

    // --- 登録 ---

    /**
     * カスタムモブの種類を登録します。
     * <p>
     * Supplier（ファクトリ）でインスタンスを毎回生成するため、
     * モブ定義クラスはコンストラクタインジェクションに依存しなくて済みます。
     *
     * @param id      モブID（config.ymlなどで参照するキー）
     * @param type    スポーンするエンティティタイプ
     * @param factory 新しいインスタンスを生成するSupplier
     */
    public void registerMob(String id, EntityType type, Supplier<CustomMob> factory) {
        mobEntityTypes.put(id, type);
        mobFactories.put(id, factory);
        if (plugin.isEnabled()) {
            log.info("[CustomMobManager] モブ登録: " + id + " (" + type.name() + ")");
        }
    }

    /** 登録されているモブIDのセットを返します。 */
    public java.util.Set<String> getRegisteredMobIds() {
        return java.util.Collections.unmodifiableSet(mobFactories.keySet());
    }

    // --- スポーン ---

    /**
     * 登録済みのエンティティタイプでカスタムモブをスポーンさせます。
     *
     * @param id  モブID
     * @param loc スポーン座標
     * @return 生成された CustomMob インスタンス、失敗時は null
     */
    public CustomMob spawnMob(String id, Location loc) {
        return spawnMob(id, loc, 1);
    }

    public CustomMob spawnMob(String id, Location loc, int level) {
        EntityType type = mobEntityTypes.getOrDefault(id, EntityType.ZOMBIE);
        return spawnMob(id, loc, type, level);
    }

    public CustomMob spawnMob(String id, Location loc, EntityType type) {
        return spawnMob(id, loc, type, 1);
    }

    public CustomMob spawnMob(String id, Location loc, EntityType type, int level) {
        Supplier<CustomMob> factory = mobFactories.get(id);
        if (factory == null) {
            log.warning("[CustomMobManager] 未登録のモブIDでスポーン試行: " + id);
            return null;
        }

        LivingEntity entity;
        try {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        } catch (ClassCastException e) {
            log.warning("[CustomMobManager] EntityType " + type + " は LivingEntity ではありません: " + id);
            return null;
        }

        try {
            CustomMob mobLogic = factory.get();
            entity.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, id);
            mobLogic.setMobId(id);
            mobLogic.setLevel(level);
            mobLogic.init(entity, this);
            activeMobs.put(entity.getUniqueId(), mobLogic);
            return mobLogic;
        } catch (Exception e) {
            log.severe("[CustomMobManager] モブ初期化に失敗しました (id=" + id + "): " + e.getMessage());
            entity.remove();
            return null;
        }
    }

    /**
     * すでに存在するエンティティにカスタムモブロジックを紐付けます。
     * (ワールドセーブ後の復元などに利用)
     */
    public void bindExistingEntity(LivingEntity entity, String id) {
        Supplier<CustomMob> factory = mobFactories.get(id);
        if (factory == null) return;

        try {
            CustomMob mobLogic = factory.get();
            entity.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, id);
            mobLogic.setMobId(id);
            mobLogic.init(entity, this);
            activeMobs.put(entity.getUniqueId(), mobLogic);
        } catch (Exception e) {
            log.severe("[CustomMobManager] エンティティへのバインドに失敗しました (id=" + id + "): " + e.getMessage());
        }
    }

    // --- クエリ ---

    /** アクティブなカスタムモブを UUID で取得します。 */
    public CustomMob getCustomMob(Entity entity) {
        if (entity == null) return null;
        return activeMobs.get(entity.getUniqueId());
    }

    /**
     * エンティティに付与されたカスタムモブIDを PDC から取得します。
     * アクティブでないモブ（チャンクアンロード後など）でも機能します。
     */
    public String getCustomMobId(Entity entity) {
        if (entity == null) return null;
        return entity.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
    }

    /** 指定IDのモブが登録されているか確認します。 */
    public boolean hasRegistration(String id) {
        return mobFactories.containsKey(id);
    }

    /** 現在アクティブなモブの数を返します。 */
    public int getActiveMobCount() {
        return activeMobs.size();
    }

    /** 現在アクティブなカスタムモブのコレクションを返します（読み取り専用）。 */
    public Collection<CustomMob> getActiveMobs() {
        return java.util.Collections.unmodifiableCollection(activeMobs.values());
    }

    /**
     * 指定した条件に合致するカスタムモブを強制デスポーンします。
     * {@link EntityDeathEvent} は発火しません（バニラドロップ・EXPは出ません）。
     * {@code onDeath()} フックも呼ばれません。
     *
     * @param locationFilter モブの座標がこのpredicateをtrueにした場合に削除
     */
    public void despawnMobsIn(java.util.function.Predicate<org.bukkit.Location> locationFilter) {
        var iterator = activeMobs.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            CustomMob mob = entry.getValue();
            if (locationFilter.test(mob.getLocation())) {
                mob.entity.remove();           // エンティティをワールドから削除
                healthManager.cleanup(entry.getKey());  // 仮想HPデータを解放
                iterator.remove();             // activeMobsから除去
            }
        }
    }


    /**
     * カスタムモブへダメージを与えたプレイヤーを記録します。
     * 死亡時のEXP付与先を決定するために使用します。
     */
    public void recordDamage(LivingEntity defender, LivingEntity attacker) {
        if (!(attacker instanceof Player player)) return;
        if (defender == null || !activeMobs.containsKey(defender.getUniqueId())) return;

        lastPlayerDamagers.put(defender.getUniqueId(), player.getUniqueId());
    }

    // --- VirtualHealth ブリッジ ---

    double getHealth(CustomMob mob) {
        return healthManager.getHealth(mob.entity);
    }

    double getMaxHealth(CustomMob mob) {
        return healthManager.getMaxHealth(mob.entity);
    }

    void setMaxHealth(CustomMob mob, double maxHp) {
        // StatManager経由ではなく直接バニラHP属性を設定してVirtualHealthManagerと同期させる
        org.bukkit.attribute.AttributeInstance attr =
                mob.entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(maxHp);
        }
        mob.entity.setHealth(maxHp);
        // VirtualHealthManagerのマップにも最大値で初期化（次のgetHealthで自動設定される）
    }

    // --- アイテムキャッシュ ---

    /**
     * キャッシュを利用してアイテムのクローンを取得します。
     * キャッシュにない場合は {@link #createItem} で生成してキャッシュに登録します。
     */
    public ItemStack getCachedItem(String itemId) {
        ItemStack cached = itemCache.computeIfAbsent(itemId, this::createItem);
        return cached != null ? cached.clone() : null;
    }

    /**
     * アイテムを新規生成します。ItemManagerへの依存が必要な場合は
     * このメソッドをオーバーライドするか、サブクラスで拡張してください。
     * デフォルト実装は null を返します（ItemManager連携は将来対応）。
     */
    protected ItemStack createItem(String itemId) {
        // TODO: ItemManagerが利用可能になったら連携する
        return null;
    }

    /** アイテムキャッシュをクリアします（リロード時などに使用）。 */
    public void clearItemCache() {
        itemCache.clear();
    }

    // --- イベントディスパッチ ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // 攻撃側がカスタムモブか確認
        CustomMob attackerMob = activeMobs.get(attacker.getUniqueId());
        if (attackerMob != null) {
            attackerMob.onAttack(victim, event);
        }

        // 防御側がカスタムモブか確認
        CustomMob victimMob = activeMobs.get(victim.getUniqueId());
        if (victimMob != null) {
            victimMob.onDamaged(attacker, event);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        UUID entityId = event.getEntity().getUniqueId();
        CustomMob mob = activeMobs.remove(entityId);
        if (mob != null) {
            // バニラのドロップを全てクリアし、onDeath()に制御を渡す
            event.getDrops().clear();
            event.setDroppedExp(0);
            grantExpReward(mob, entityId);
            mob.onDeath();
        }
        // VirtualHealthManagerのメモリを解放
        lastPlayerDamagers.remove(entityId);
        healthManager.cleanup(entityId);
    }

    private static final double PARTY_BONUS_RATIO = 0.5;

    private void grantExpReward(CustomMob mob, UUID entityId) {
        int exp = mob.getExp();
        if (exp <= 0) return;

        UUID playerId = lastPlayerDamagers.get(entityId);
        if (playerId == null) return;

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        playerManager.addExp(player, exp);
        distributePartyExp(player, exp);
    }

    private void distributePartyExp(Player killer, int baseExp) {
        Party party = partyManager.getParty(killer);
        if (party == null) return;

        int bonusExp = (int) (baseExp * PARTY_BONUS_RATIO);
        if (bonusExp <= 0) return;

        for (Player member : party.getOnlineMembers()) {
            if (member.getUniqueId().equals(killer.getUniqueId())) continue;
            if (!member.getWorld().equals(killer.getWorld())) continue;
            playerManager.addExp(member, bonusExp);
            member.sendMessage(
                    net.kyori.adventure.text.Component.text("§a+ " + String.format("%,d", bonusExp) + " EXP (パーティーボーナス)")
            );
        }
    }

    @EventHandler
    public void onVirtualHealthChange(com.ruskserver.deepwither_V2.modules.combat.health.event.VirtualHealthChangeEvent event) {
        CustomMob mob = activeMobs.get(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.updateDisplayName();
        }
    }
}
