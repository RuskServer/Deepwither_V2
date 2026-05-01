package com.ruskserver.deepwither_V2.modules.mob.framework;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
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
    private final Logger log;

    /** mob-id → (EntityType, Supplierファクトリ) のレジストリ */
    private final Map<String, EntityType> mobEntityTypes = new ConcurrentHashMap<>();
    private final Map<String, Supplier<CustomMob>> mobFactories = new ConcurrentHashMap<>();

    /** 現在アクティブなモブ: UUID → CustomMobインスタンス */
    private final Map<UUID, CustomMob> activeMobs = new ConcurrentHashMap<>();

    /** 装備アイテムキャッシュ: itemId → クローン元ItemStack */
    private final Map<String, ItemStack> itemCache = new ConcurrentHashMap<>();

    /** エンティティへのモブIDタグに使用するキー */
    private final NamespacedKey mobIdKey;

    @Inject
    public CustomMobManager(JavaPlugin plugin, VirtualHealthManager healthManager) {
        this.plugin = plugin;
        this.healthManager = healthManager;
        this.log = plugin.getLogger();
        this.mobIdKey = new NamespacedKey(plugin, "custom_mob_id");
    }

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
        EntityType type = mobEntityTypes.getOrDefault(id, EntityType.ZOMBIE);
        return spawnMob(id, loc, type);
    }

    /**
     * エンティティタイプを指定してカスタムモブをスポーンさせます。
     */
    public CustomMob spawnMob(String id, Location loc, EntityType type) {
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
        CustomMob mob = activeMobs.remove(event.getEntity().getUniqueId());
        if (mob != null) {
            // バニラのドロップを全てクリアし、onDeath()に制御を渡す
            event.getDrops().clear();
            event.setDroppedExp(0);
            mob.onDeath();
        }
        // VirtualHealthManagerのメモリを解放
        healthManager.cleanup(event.getEntity().getUniqueId());
    }
}
