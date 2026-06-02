package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * グール — ゲーム序盤の基本敵モブ。
 *
 * <p><b>ステータス</b>
 * <ul>
 *   <li>HP: 18
 *   <li>物理攻撃力: 3（DamagePipelineManagerを経由）
 * </ul>
 *
 * <p><b>スキル</b>
 * <ul>
 *   <li><b>飛び掛かり (Pounce)</b>: 6ブロック以内のプレイヤーへ跳躍し、着地時に3ダメージ（クールダウン: 10〜15秒）
 *   <li><b>突進 (Charge)</b>: 10ブロック以内のプレイヤーへ一直線に突進し、パーティクル煙を巻き上げる（クールダウン: 20秒）
 * </ul>
 */
@Component
public class GhoulMob extends CustomMob {

    // --- ステータス定数 ---
    private static final double MAX_HP        = 18.0;
    private static final double ATTACK_DAMAGE = 3.0;
    private static final int EXP_REWARD       = 50;

    // --- スキルクールダウン (tick) ---
    private static final int POUNCE_COOLDOWN_MIN = 200;  // 10秒
    private static final int POUNCE_COOLDOWN_MAX = 300;  // 15秒
    private static final int CHARGE_COOLDOWN     = 400;  // 20秒

    // --- スキルの射程 (ブロック) ---
    private static final double POUNCE_RANGE = 6.0;
    private static final double CHARGE_RANGE = 10.0;

    // --- スキルのパラメーター ---
    private static final double POUNCE_HIT_RADIUS   = 2.5;  // 着地時のヒット判定
    private static final double POUNCE_POWER_XZ     = 0.9;  // 飛び掛かりの水平速度
    private static final double POUNCE_POWER_Y      = 0.6;  // 飛び掛かりの垂直速度
    private static final double CHARGE_POWER        = 1.3;  // 突進の速度

    // --- スキル状態管理 ---
    private int pounceCooldown = 60;          // 初回は3秒後から発動可能
    private int chargeCooldown = 80;          // 初回は4秒後から発動可能
    private boolean pouncing = false;         // 飛び掛かり中フラグ
    private Player pounceTarget = null;       // 飛び掛かり対象

    private final DamagePipelineManager damageManager;
    private final ItemManager itemManager;

    @Inject
    public GhoulMob(CustomMobManager mobManager, DamagePipelineManager damageManager, ItemManager itemManager) {
        // 自己登録：スポーン時にファクトリで新しいインスタンスを生成する
        mobManager.registerMob("ghoul", EntityType.ZOMBIE,
                () -> new GhoulMob(mobManager, damageManager, itemManager));
        this.damageManager = damageManager;
        this.itemManager = itemManager;
    }

    // =========================================================
    // フックメソッド
    // =========================================================

    @Override
    public void onSpawn() {
        setMaxHealth(MAX_HP);
        setExp(EXP_REWARD);

        // 表示名
        entity.customName(net.kyori.adventure.text.Component.text("グール")
                .color(net.kyori.adventure.text.format.TextColor.color(0x7B4F2E)));
        entity.setCustomNameVisible(true);

        // Zombie固有の設定
        if (entity instanceof Zombie zombie) {
            zombie.setBaby(false);
            zombie.setShouldBurnInDay(false);
        }

        // 攻撃力はDamagePipelineManagerで管理するため、バニラ攻撃力を0にする
        var attackAttr = entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
        if (attackAttr != null) attackAttr.setBaseValue(0.0);

        // 移動速度をデフォルトのZombie（0.23）に戻す
        var speedAttr = entity.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.23);
    }

    @Override
    public void onTick() {
        if (pouncing) {
            checkPounceHit();
            return;  // 飛び掛かり中は他のスキルを発動しない
        }

        // クールダウンを減算
        if (pounceCooldown > 0) pounceCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;

        // スキル発動チェック（5tickごとに実行してCPU負荷を削減）
        if (ticksLived % 5 != 0) return;

        if (pounceCooldown == 0) {
            tryPounce();
        } else if (chargeCooldown == 0) {
            tryCharge();
        }
    }

    @Override
    public void onDeath() {
        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.03);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_DEATH, 1.0f, 0.7f);

        // グールの残滓をドロップ (20%の確率)
        if (RANDOM.nextDouble() < 0.2) {
            org.bukkit.inventory.ItemStack remnant = itemManager.generate("ghoul_remnant");
            if (remnant != null) {
                loc.getWorld().dropItemNaturally(getLocation(), remnant);
            }
        }

        // グールの内臓をドロップ (30%の確率)
        if (RANDOM.nextDouble() < 0.3) {
            org.bukkit.inventory.ItemStack viscera = itemManager.generate("ghoul_viscera");
            if (viscera != null) {
                loc.getWorld().dropItemNaturally(getLocation(), viscera);
            }
        }

        // グールの精髄をドロップ (10%の確率)
        if (RANDOM.nextDouble() < 0.1) {
            org.bukkit.inventory.ItemStack essence = itemManager.generate("ghoul_essence");
            if (essence != null) {
                loc.getWorld().dropItemNaturally(getLocation(), essence);
            }
        }

        // 1%の確率でアーティファクトボックスをドロップ
        if (RANDOM.nextDouble() < 0.01) {
            org.bukkit.inventory.ItemStack box = itemManager.generate("artifact_box");
            if (box != null) {
                loc.getWorld().dropItemNaturally(getLocation(), box);
            }
        }
    }

    @Override
    public void onAttack(LivingEntity victim, org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        // バニラのダメージ処理はキャンセルされているため、自分でDamagePipelineに流す
        if (victim instanceof Player player) {
            damageManager.processDamage(entity, player, DamageType.PHYSICAL, ATTACK_DAMAGE, null);
        }
    }

    // =========================================================
    // スキル実装
    // =========================================================

    /**
     * 飛び掛かり (Pounce)
     * 近くのプレイヤーへ向かってジャンプする。
     * 着地した瞬間にヒット判定を行い、近くにいるプレイヤーへダメージを与える。
     */
    private void tryPounce() {
        Player target = getNearestPlayer(POUNCE_RANGE);
        if (target == null) return;

        // ジャンプ方向を計算
        Vector dir = target.getLocation().subtract(getLocation()).toVector();
        if (dir.lengthSquared() < 0.01) return;
        dir.normalize().setY(POUNCE_POWER_Y).multiply(POUNCE_POWER_XZ);
        dir.setY(POUNCE_POWER_Y);  // Y成分は倍率と独立させる

        entity.setVelocity(dir);

        // エフェクト
        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.DUST,
                loc.add(0, 0.5, 0), 8, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(0x5C3317), 1.5f));
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.4f);

        pounceTarget = target;
        pouncing = true;
        // 次回クールダウンをランダム設定
        pounceCooldown = POUNCE_COOLDOWN_MIN + RANDOM.nextInt(POUNCE_COOLDOWN_MAX - POUNCE_COOLDOWN_MIN + 1);
    }

    /**
     * 飛び掛かり着地判定。毎tick呼ばれ、着地したらヒット判定を行う。
     */
    private void checkPounceHit() {
        // 地面に着いたか、または空中に5秒以上いたら終了
        if (ticksLived % 3 != 0) return;  // 3tickごとにチェック

        if (entity.isOnGround()) {
            pouncing = false;

            if (pounceTarget != null && pounceTarget.isOnline() && !pounceTarget.isDead()) {
                double dist = pounceTarget.getLocation().distance(getLocation());
                if (dist <= POUNCE_HIT_RADIUS) {
                    damageManager.processDamage(entity, pounceTarget, DamageType.PHYSICAL, ATTACK_DAMAGE, null);
                    // 着地ヒットエフェクト
                    Location loc = getLocation();
                    loc.getWorld().spawnParticle(Particle.CRIT, loc.add(0, 0.5, 0), 15, 0.5, 0.5, 0.5, 0.2);
                    loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
                }
            }
            pounceTarget = null;
        }
    }

    /**
     * 突進 (Charge)
     * 少し遠くのプレイヤーへ向かって勢いよく突進する。
     * 突進自体にはダメージがなく、バニラのAI（近接攻撃）に当たり判定を任せる。
     */
    private void tryCharge() {
        Player target = getNearestPlayer(CHARGE_RANGE);
        if (target == null) return;

        Vector dir = target.getLocation().subtract(getLocation()).toVector();
        if (dir.lengthSquared() < 0.01) return;
        dir.setY(0).normalize().multiply(CHARGE_POWER);

        entity.setVelocity(dir);

        // エフェクト（突進の煙）
        Location loc = getLocation();
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 0.5, 0), 15, 0.4, 0.2, 0.4, 0.08);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 0.6f);

        chargeCooldown = CHARGE_COOLDOWN;
    }

    // =========================================================
    // ユーティリティ
    // =========================================================

    /**
     * 指定した半径内で最も近くにいるプレイヤーを返します。
     * 死亡・オフラインのプレイヤーは除外します。
     *
     * @param radius 検索半径（ブロック）
     * @return 最も近いプレイヤー、いなければ null
     */
    private Player getNearestPlayer(double radius) {
        List<Player> nearby = entity.getWorld().getPlayers().stream()
                .filter(p -> !p.isDead() && p.getLocation().distanceSquared(getLocation()) <= radius * radius)
                .sorted((a, b) -> Double.compare(
                        a.getLocation().distanceSquared(getLocation()),
                        b.getLocation().distanceSquared(getLocation())))
                .toList();
        return nearby.isEmpty() ? null : nearby.get(0);
    }
}
