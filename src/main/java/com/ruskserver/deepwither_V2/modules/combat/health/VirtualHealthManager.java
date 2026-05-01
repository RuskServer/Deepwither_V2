package com.ruskserver.deepwither_V2.modules.combat.health;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.combat.feedback.DamageFeedbackService;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * マイクラ標準の体力属性を無視し、完全に独立した「仮想HP」を管理するサービス。
 * エンティティの受けるダメージや回復はすべてこのクラスを経由して処理されます。
 */
@Service
public class VirtualHealthManager {

    private final Map<UUID, Double> currentHealthMap = new ConcurrentHashMap<>();
    private final StatManager statManager;
    private final DamageFeedbackService damageFeedbackService;

    @Inject
    public VirtualHealthManager(StatManager statManager, DamageFeedbackService damageFeedbackService) {
        this.statManager = statManager;
        this.damageFeedbackService = damageFeedbackService;
    }

    /**
     * エンティティの現在の仮想HPを取得します。
     * マップに存在しない場合は、最大HPを初期値として設定して返します。
     */
    public double getHealth(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        if (!currentHealthMap.containsKey(id)) {
            double maxHp = getMaxHealth(entity);
            currentHealthMap.put(id, maxHp);
            return maxHp;
        }
        return currentHealthMap.get(id);
    }

    /**
     * エンティティの最大仮想HPを取得します。
     */
    public double getMaxHealth(LivingEntity entity) {
        double maxHp = statManager.getTotalStat(entity, StatType.HEALTH);
        // 万が一最大HPが0以下の場合は最低1を保証する
        return maxHp > 0 ? maxHp : 1.0;
    }

    /**
     * エンティティの仮想HPを回復させます。
     */
    public void heal(LivingEntity entity, double amount) {
        if (entity == null || entity.isDead() || amount <= 0) return;

        double current = getHealth(entity);
        double maxHp = getMaxHealth(entity);
        double newHealth = Math.min(current + amount, maxHp);
        
        currentHealthMap.put(entity.getUniqueId(), newHealth);
        syncVisualHealth(entity, newHealth, maxHp);
    }

    /**
     * エンティティに仮想ダメージを与えます。
     * HPが0になった場合は対象を死亡させます。
     */
    public void damage(LivingEntity entity, double amount) {
        if (entity == null || entity.isDead() || amount <= 0) return;

        double current = getHealth(entity);
        double maxHp = getMaxHealth(entity);
        double newHealth = current - amount;
        damageFeedbackService.playHurtFeedback(entity);

        if (newHealth <= 0) {
            newHealth = 0;
            currentHealthMap.put(entity.getUniqueId(), newHealth);
            // マイクラ側で対象を殺す
            entity.setHealth(0);
        } else {
            currentHealthMap.put(entity.getUniqueId(), newHealth);
            syncVisualHealth(entity, newHealth, maxHp);
        }
    }

    /**
     * エンティティがシステムから削除された（ログアウト、チャンクアンロード、死亡など）際にメモリを解放します。
     */
    public void cleanup(UUID entityId) {
        currentHealthMap.remove(entityId);
    }

    /**
     * 仮想HPの割合に合わせて、マイクラクライアント上の体力表示（ハート）を同期します。
     */
    private void syncVisualHealth(LivingEntity entity, double currentHp, double maxHp) {
        if (entity instanceof Player player) {
            // プレイヤーの場合、画面上のハート(20.0スケール)を変動させる
            double ratio = currentHp / maxHp;
            double visualHealth = 20.0 * ratio;
            
            // 安全のために0〜20の範囲に収める
            visualHealth = Math.max(0.1, Math.min(visualHealth, 20.0));
            
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null && maxHealthAttr.getBaseValue() != 20.0) {
                maxHealthAttr.setBaseValue(20.0);
            }
            
            player.setHealth(visualHealth);
        } else {
            // Mobの場合も、必要に応じてバニラのHPバーなどを同期させる（今回はPlayerと同様の割合同期を行う）
            AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double baseMax = maxHealthAttr.getBaseValue();
                double ratio = currentHp / maxHp;
                double visualHealth = baseMax * ratio;
                visualHealth = Math.max(0.1, Math.min(visualHealth, baseMax));
                entity.setHealth(visualHealth);
            }
        }
    }
}
