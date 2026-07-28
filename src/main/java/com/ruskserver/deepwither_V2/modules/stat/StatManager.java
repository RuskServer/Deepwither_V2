package com.ruskserver.deepwither_V2.modules.stat;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * サーバー上のすべてのエンティティのステータスを一元管理するサービス。
 */
@Service
public class StatManager {

    private static final double DEFAULT_MOVEMENT_SPEED = 0.1;
    private static final double MIN_MOVEMENT_SPEED = 0.02;
    private static final double MAX_MOVEMENT_SPEED = 0.30;
    private final Map<UUID, StatProfile> activeProfiles = new HashMap<>();

    /**
     * エンティティのStatProfileを取得します。存在しない場合は新規作成します。
     */
    public StatProfile getProfile(UUID entityId) {
        return activeProfiles.computeIfAbsent(entityId, k -> new StatProfile());
    }

    /**
     * エンティティのStatProfileを削除します（ログアウト時や死亡時など）。
     */
    public void removeProfile(UUID entityId) {
        activeProfiles.remove(entityId);
    }

    /**
     * エンティティにモディファイアを付与します。
     */
    public void setModifier(UUID entityId, StatType type, String sourceId, double value, ModifierType modType) {
        getProfile(entityId).setModifier(type, sourceId, value, modType);
        syncMovementSpeed(entityId, type);
    }

    /**
     * エンティティから指定ソースのモディファイアを削除します。
     */
    public void removeModifier(UUID entityId, StatType type, String sourceId) {
        getProfile(entityId).removeModifier(type, sourceId);
        syncMovementSpeed(entityId, type);
    }

    /**
     * エンティティの現在の指定ステータスの最終合計値を取得します。
     */
    public double getTotalStat(UUID entityId, StatType type) {
        return getProfile(entityId).getTotal(type);
    }

    /**
     * エンティティの現在の指定ステータスの最終合計値を取得します。（LivingEntityを渡す便利メソッド）
     */
    public double getTotalStat(LivingEntity entity, StatType type) {
        if (entity == null) return 0.0;
        return getTotalStat(entity.getUniqueId(), type);
    }

    private void syncMovementSpeed(UUID entityId, StatType changedType) {
        if (changedType != StatType.SPEED) return;
        Player player = Bukkit.getPlayer(entityId);
        if (player == null) return;

        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) return;

        double speedBonusPercent = getProfile(entityId).getTotal(StatType.SPEED);
        double speed = DEFAULT_MOVEMENT_SPEED * (1.0 + speedBonusPercent / 100.0);
        attribute.setBaseValue(Math.max(MIN_MOVEMENT_SPEED, Math.min(MAX_MOVEMENT_SPEED, speed)));
    }
}
