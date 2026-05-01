package com.ruskserver.deepwither_V2.modules.combat.damage;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.phases.DamagePhase;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * バニラのダメージ処理をインターセプトし、パイプラインを実行して仮想HPに反映させるマネージャー。
 */
@Component
public class DamagePipelineManager implements Listener {

    private final VirtualHealthManager healthManager;
    private final StatManager statManager;
    private final List<DamagePhase> pipeline = new ArrayList<>();

    @Inject
    public DamagePipelineManager(VirtualHealthManager healthManager, StatManager statManager) {
        this.healthManager = healthManager;
        this.statManager = statManager;

        // パイプラインのフェーズを順番に登録する
        // 1. 基礎ダメージの設定
        pipeline.add(new DamagePhase.Base(statManager));
        // 2. クリティカルの判定
        pipeline.add(new DamagePhase.Critical(statManager));
        // 3. 防御力による軽減
        pipeline.add(new DamagePhase.Defense(statManager));
        // 必要に応じてここに吸血処理やシールド処理のフェーズを追加できます
    }

    /**
     * エンティティ同士の戦闘ダメージ処理。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity defender)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        // バニラのダメージ計算を完全にキャンセル
        event.setDamage(0);
        event.setCancelled(true);

        // ダメージタイプの判定（今回はデフォルトでPHYSICALとする。魔法アイテムならMAGICにするなどの拡張が可能）
        DamageType type = DamageType.PHYSICAL;

        // パイプラインを通すためのコンテキストを生成
        DamageContext context = new DamageContext(attacker, defender, type, 0.0);

        // パイプライン処理を実行
        for (DamagePhase phase : pipeline) {
            phase.process(context);
        }

        // 最終ダメージを仮想HPから減算
        if (context.getDamage() > 0) {
            healthManager.damage(defender, context.getDamage());
            
            // TODO: クリティカル発生時のエフェクトや、ダメージホログラム(HolographicDisplaysなど)の表示
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

        // バニラの計算をキャンセル
        double vanillaDamage = event.getDamage();
        event.setDamage(0);
        event.setCancelled(true);

        if (vanillaDamage <= 0) return;

        // バニラダメージの重み（20.0を基準とする）を元に、最大仮想HPに対する「割合ダメージ」に変換する
        // 例：落下ダメージがバニラで「4(ハート2個分)」の場合、4/20 = 0.2 (最大HPの20%)のダメージとなる
        double ratio = vanillaDamage / 20.0;
        double maxHp = healthManager.getMaxHealth(defender);
        double finalDamage = maxHp * ratio;

        if (finalDamage > 0) {
            healthManager.damage(defender, finalDamage);
        }
    }
}
