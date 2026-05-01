package com.ruskserver.deepwither_V2.modules.mob.framework;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.Random;
import java.util.function.Consumer;

/**
 * カスタムモブのロジックを定義する基底クラス。
 * <p>
 * 各モブ実装クラスはこのクラスを継承し、必要なフックメソッドをオーバーライドしてください。
 * インスタンスの生成は {@link CustomMobManager} が担当します。
 */
public abstract class CustomMob {

    protected LivingEntity entity;
    protected UUID uuid;
    protected String mobId;
    protected int ticksLived = 0;
    protected CustomMobManager manager;

    protected static final Random RANDOM = new Random();

    /**
     * モブが初期化される際に呼び出されます。
     * マネージャーから内部的に呼ばれるメソッドです。直接呼ばないでください。
     */
    final void init(LivingEntity entity, CustomMobManager manager) {
        this.entity = entity;
        this.uuid = entity.getUniqueId();
        this.manager = manager;
        onSpawn();
    }

    final void setMobId(String mobId) {
        this.mobId = mobId;
    }

    /**
     * 毎tick呼び出されるロジック更新メソッド。
     */
    final void tick() {
        if (entity == null || !entity.isValid()) return;
        ticksLived++;
        onTick();
    }

    // --- フックメソッド (サブクラスでオーバーライド) ---

    /** スポーン時に一度だけ呼ばれます。HP設定や装備などの初期化を行ってください。 */
    public void onSpawn() {}

    /** 毎tick呼ばれます。AIの追加ロジックや定期スキル発動などに使用します。 */
    public void onTick() {}

    /** 死亡時に呼ばれます。ドロップアイテムの追加などを行ってください。 */
    public void onDeath() {}

    /**
     * このモブが他エンティティに攻撃を与えた時に呼ばれます。
     *
     * @param victim  攻撃対象
     * @param event   バニラのダメージイベント（既にキャンセル済み）
     */
    public void onAttack(LivingEntity victim, org.bukkit.event.entity.EntityDamageByEntityEvent event) {}

    /**
     * このモブがダメージを受けた時に呼ばれます。
     *
     * @param attacker 攻撃者（null の場合は環境ダメージ）
     * @param event    バニラのダメージイベント（既にキャンセル済み）
     */
    public void onDamaged(LivingEntity attacker, org.bukkit.event.entity.EntityDamageByEntityEvent event) {}

    // --- ユーティリティ ---

    public LivingEntity getEntity() { return entity; }
    public UUID getUniqueId() { return uuid; }
    public String getMobId() { return mobId; }
    public int getTicksLived() { return ticksLived; }

    public Location getLocation() {
        return entity.getLocation();
    }

    /** 仮想HPを取得します。 */
    public double getHealth() {
        return manager.getHealth(this);
    }

    /** 最大仮想HPを取得します。 */
    public double getMaxHealth() {
        return manager.getMaxHealth(this);
    }

    /**
     * 最大仮想HPを設定し、現在HPも最大値にリセットします。
     * onSpawn() 内で呼び出して固有HPを設定してください。
     */
    public void setMaxHealth(double health) {
        manager.setMaxHealth(this, health);
    }

    /**
     * キャッシュを利用して防具・武器を装備します。
     * キャッシュにない場合は新たに生成して保存されます。
     *
     * @param itemId     アイテムID
     * @param slotSetter 装備スロットを設定するConsumer (例: {@code entity.getEquipment()::setHelmet})
     * @return アイテムが存在し装備できた場合 true
     */
    protected boolean equipIfPresent(String itemId, Consumer<ItemStack> slotSetter) {
        ItemStack item = manager.getCachedItem(itemId);
        if (item == null) return false;
        slotSetter.accept(item);
        return true;
    }

    /**
     * ランダムなドロップを行います。チャンス判定付き。
     * ランダムなステータスを持つ可能性があるため、毎回新たにアイテムを生成します。
     *
     * @param itemId アイテムID
     * @param chance ドロップ確率 (0.0〜1.0)
     * @param loc    ドロップ座標
     */
    protected void dropIfPresent(String itemId, double chance, Location loc) {
        if (RANDOM.nextDouble() >= chance) return;
        ItemStack item = manager.createItem(itemId);
        if (item != null) {
            loc.getWorld().dropItemNaturally(loc, item);
        }
    }
}
