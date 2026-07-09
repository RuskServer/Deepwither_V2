package com.ruskserver.deepwither_V2.modules.mob.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
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
 *   <li><b>飛び掛かり (Pounce)</b>: 6ブロック以内のプレイヤーへ跳躍し、着地時に3ダメージ（クールダウン: 14〜20秒、発動前に0.8〜1.0秒の溜めあり）
 *   <li><b>突進 (Charge)</b>: 10ブロック以内のプレイヤーへ一直線に突進し、パーティクル煙を巻き上げる（クールダウン: 28秒、発動前に0.5秒の溜めあり）
 * </ul>
 */
@Component
public class GhoulMob extends CustomMob {

    // --- ステータス定数 ---
    private static final double MAX_HP        = 18.0;
    private static final double ATTACK_DAMAGE = 3.0;
    private static final int EXP_REWARD       = 50;

     // --- スキルクールダウン (tick) ---
    private static final int POUNCE_COOLDOWN_MIN = 280;  // 14秒
    private static final int POUNCE_COOLDOWN_MAX = 400;  // 20秒
    private static final int CHARGE_COOLDOWN     = 560;  // 28秒

    // --- スキルの射程 (ブロック) ---
    private static final double POUNCE_RANGE = 6.0;
    private static final double CHARGE_RANGE = 10.0;

    // --- スキルのパラメーター ---
    private static final double POUNCE_HIT_RADIUS   = 2.0;  // 着地時のヒット判定
    private static final double POUNCE_POWER_XZ     = 0.9;  // 飛び掛かりの水平速度
    private static final double POUNCE_POWER_Y      = 0.6;  // 飛び掛かりの垂直速度
    private static final double CHARGE_POWER        = 1.3;  // 突進の速度

    // --- 予兆（溜め）フェーズ ---
    private static final int POUNCE_WINDUP_MIN = 16;  // 0.8秒
    private static final int POUNCE_WINDUP_MAX = 20;  // 1.0秒
    private static final int CHARGE_WINDUP     = 10;  // 0.5秒

    // --- スキル状態管理 ---
    private int pounceCooldown = 60;          // 初回は3秒後から発動可能
    private int chargeCooldown = 80;          // 初回は4秒後から発動可能
    private boolean pouncing = false;         // 飛び掛かり中フラグ
    private Player pounceTarget = null;       // 飛び掛かり対象
    private boolean pounceWinding = false;    // 飛び掛かりの溜め中フラグ
    private int pounceWindup = 0;             // 飛び掛かりの溜め残りtick
    private Player pounceWindTarget = null;   // 溜め中に狙っている対象
    private boolean chargeWinding = false;    // 突進の溜め中フラグ
    private int chargeWindup = 0;             // 突進の溜め残りtick
    private Player chargeWindTarget = null;   // 溜め中に狙っている対象

    private final DamagePipelineManager damageManager;
    private final ItemManager itemManager;

    @Inject
    public GhoulMob(CustomMobManager mobManager, DamagePipelineManager damageManager, ItemManager itemManager) {
        // 自己登録：スポーン時にファクトリで新しいインスタンスを生成する
        mobManager.registerMob("ghoul", EntityType.ZOMBIE,
                () -> new GhoulMob(mobManager, damageManager, itemManager));
        mobManager.registerDisplayName("ghoul", "グール");
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

        // 溜め中のスキルを進める
        if (pounceWinding) {
            tickPounceWindup();
            return;
        }
        if (chargeWinding) {
            tickChargeWindup();
            return;
        }

        // クールダウンを減算
        if (pounceCooldown > 0) pounceCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;

        // スキル発動チェック（5tickごとに実行してCPU負荷を削減）
        if (ticksLived % 5 != 0) return;

        if (pounceCooldown == 0) {
            startPounceWindup();
        } else if (chargeCooldown == 0) {
            startChargeWindup();
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
    public double getBaseAttackDamage() {
        return ATTACK_DAMAGE;
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
     * 飛び掛かり (Pounce) の予兆開始。
     * 即座に跳躍せず、短い溜めフェーズで予告エフェクトを出す。
     */
    private void startPounceWindup() {
        Player target = getNearestPlayer(POUNCE_RANGE);
        if (target == null) return;

        pounceWindTarget = target;
        pounceWinding = true;
        pounceWindup = POUNCE_WINDUP_MIN + RANDOM.nextInt(POUNCE_WINDUP_MAX - POUNCE_WINDUP_MIN + 1);

        // 予兆エフェクト：足元に赤茶色のチャージリング
        Location loc = getLocation();
        TrailCircleHelper.spawnCircle(loc.add(0, 0.1, 0), POUNCE_HIT_RADIUS + 0.5,
                Color.fromRGB(0x9C4A2A), 14, 28);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_AMBIENT, 0.7f, 0.6f);
    }

    /**
     * 飛び掛かりの溜めを1tick進める。終了時に実際のジャンプを実行する。
     */
    private void tickPounceWindup() {
        if (pounceWindup > 0) {
            pounceWindup--;
            return;
        }

        Player target = pounceWindTarget;
        pounceWinding = false;
        pounceWindTarget = null;
        if (target == null || !target.isOnline() || target.isDead()) {
            // ターゲットが消えたらスキルをキャンセルし、短いクールダウンを置く
            pounceCooldown = 40;
            return;
        }

        // ジャンプ方向を計算
        Vector dir = target.getLocation().subtract(getLocation()).toVector();
        if (dir.lengthSquared() < 0.01) {
            pounceCooldown = 40;
            return;
        }
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
     * 突進 (Charge) の予兆開始。
     * 短い溜めフェーズで前方への予告エフェクトを出す。
     */
    private void startChargeWindup() {
        Player target = getNearestPlayer(CHARGE_RANGE);
        if (target == null) return;

        chargeWindTarget = target;
        chargeWinding = true;
        chargeWindup = CHARGE_WINDUP;

        // 予兆エフェクト：ターゲット方向へ向けた煙の帯
        Location from = getLocation().add(0, 0.5, 0);
        Vector dir = target.getLocation().subtract(getLocation()).toVector();
        if (dir.lengthSquared() < 0.01) return;
        dir.setY(0).normalize();
        Location to = from.clone().add(dir.multiply(CHARGE_POWER * 3 + 1.5));
        com.ruskserver.deepwither_V2.modules.skill.util.TrailHelper.spawnLine(from, to,
                Color.fromRGB(0x6E6E6E), 10);
    }

    /**
     * 突進の溜めを1tick進める。終了時に実際の突進を実行する。
     */
    private void tickChargeWindup() {
        if (chargeWindup > 0) {
            chargeWindup--;
            return;
        }

        Player target = chargeWindTarget;
        chargeWinding = false;
        chargeWindTarget = null;

        if (target != null && target.isOnline() && !target.isDead()) {
            Vector dir = target.getLocation().subtract(getLocation()).toVector();
            if (dir.lengthSquared() >= 0.01) {
                dir.setY(0).normalize().multiply(CHARGE_POWER);
                entity.setVelocity(dir);

                // エフェクト（突進の煙）
                Location loc = getLocation();
                loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 0.5, 0), 15, 0.4, 0.2, 0.4, 0.08);
                loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 0.6f);
            }
        }

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
