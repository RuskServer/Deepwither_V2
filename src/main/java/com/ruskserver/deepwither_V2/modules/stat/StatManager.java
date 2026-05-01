package com.ruskserver.deepwither_V2.modules.stat;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * サーバー上のすべてのエンティティのステータスを一元管理するサービス。
 */
@Service
public class StatManager {

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
    }

    /**
     * エンティティから指定ソースのモディファイアを削除します。
     */
    public void removeModifier(UUID entityId, StatType type, String sourceId) {
        getProfile(entityId).removeModifier(type, sourceId);
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
}
