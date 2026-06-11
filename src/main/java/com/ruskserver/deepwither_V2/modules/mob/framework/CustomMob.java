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
    private int exp = 0;

    protected int level = 1;
    protected double baseMaxHealth = 20.0;
    protected String baseName = null;

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
        
        // onSpawn() の後にベース名を判定して名前表示を適用する
        if (baseName == null) {
            net.kyori.adventure.text.Component customName = entity.customName();
            if (customName != null) {
                baseName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(customName);
            } else {
                baseName = entity.getName();
            }
        }
        updateDisplayName();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
        updateDisplayName();
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
    public int getExp() { return exp; }

    protected void setExp(int exp) {
        this.exp = Math.max(0, exp);
    }

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
        this.baseMaxHealth = health;
        double multiplier = manager.getHealthMultiplier();
        double finalHealth = health * (1.0 + (level - 1) * multiplier);
        manager.setMaxHealth(this, finalHealth);
        updateDisplayName();
    }

    public void updateDisplayName() {
        if (entity == null || manager == null) return;
        String format = manager.getNameFormat();
        if (format == null || format.isBlank()) return;

        double currentHp = getHealth();
        double maxHp = getMaxHealth();
        String currentBaseName = baseName != null ? baseName : entity.getName();

        String formatted = format
                .replace("{level}", String.valueOf(level))
                .replace("{name}", currentBaseName)
                .replace("{hp}", String.format("%.0f", currentHp))
                .replace("{max_hp}", String.format("%.0f", maxHp));

        net.kyori.adventure.text.Component nameComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(formatted);
        entity.customName(nameComponent);
        entity.setCustomNameVisible(true);
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

    /**
     * このモブの基本近接攻撃力を返します。
     * サブクラスでオーバーライドして固有の攻撃力を定義してください。
     * この値は StatManager に登録され、ダメージパイプラインの基礎値として使用されます。
     *
     * @return 基本攻撃力（デフォルト: 0）
     */
    public double getBaseAttackDamage() {
        return 0.0;
    }
}
