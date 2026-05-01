package com.ruskserver.deepwither_V2.core.database.player;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ruskserver.deepwither_V2.core.database.DatabaseManager;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.repository.CachedRepository;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PlayerDataRepository extends CachedRepository<UUID, PlayerData> {

    private final Logger logger;
    private final DIContainer container;
    private final List<PlayerDataProvider<?>> providers = new ArrayList<>();
    private boolean providersLoaded = false;

    @Inject
    public PlayerDataRepository(DatabaseManager db, DIContainer container) {
        super(db);
        this.logger = Logger.getLogger("PlayerDataRepo");
        this.container = container;
        // プロバイダーの収集は遅延初期化（全コンポーネント登録後に確実に行う）
    }

    /**
     * 全コンポーネントが登録された後に初めて呼ばれた際、プロバイダーを収集します。
     */
    private void ensureProvidersLoaded() {
        if (providersLoaded) return;
        providersLoaded = true;

        for (Object instance : container.getAllInstances()) {
            if (instance instanceof PlayerDataProvider) {
                providers.add((PlayerDataProvider<?>) instance);
                logger.info("Registered PlayerDataProvider: " + instance.getClass().getSimpleName());
            }
        }
    }

    @Override
    protected Cache<UUID, PlayerData> buildCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(30, TimeUnit.MINUTES) // プレイヤーログアウト後30分でキャッシュから破棄
                .build();
    }

    @Override
    protected Optional<PlayerData> loadFromDb(UUID key) {
        ensureProvidersLoaded();
        PlayerData playerData = new PlayerData(key);

        try (Connection conn = db.getConnection()) {
            for (PlayerDataProvider<?> provider : providers) {
                try {
                    Object data = provider.loadFromDb(key, conn);
                    if (data != null) {
                        // Cast is safe because provider creates data matching its own getKey type
                        @SuppressWarnings("unchecked")
                        PlayerDataProvider<Object> objProvider = (PlayerDataProvider<Object>) provider;
                        playerData.setInitial(objProvider.getKey(), data);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load data from provider: " + provider.getClass().getSimpleName(), e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get connection for loading PlayerData", e);
        }

        return Optional.of(playerData);
    }

    @Override
    protected void saveToDb(UUID key, PlayerData value) {
        ensureProvidersLoaded();
        Set<DataKey<?>> dirtyKeys = value.getDirtyKeys();

        if (dirtyKeys.isEmpty()) {
            return; // 変更がなければ保存処理をスキップ
        }

        try (Connection conn = db.getConnection()) {
            // Auto-commit を false にしてトランザクションを開始することも可能ですが、
            // 各モジュールが別のテーブルにアクセスするため、単純化のために標準で実行します。
            
            for (PlayerDataProvider<?> provider : providers) {
                if (dirtyKeys.contains(provider.getKey())) {
                    try {
                        @SuppressWarnings("unchecked")
                        PlayerDataProvider<Object> objProvider = (PlayerDataProvider<Object>) provider;
                        Object data = value.get(objProvider.getKey());
                        
                        objProvider.saveToDb(key, data, conn);
                        
                        // 保存が成功したキーのDirtyフラグを下ろす
                        value.clearDirtyFlag(objProvider.getKey());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to save data from provider: " + provider.getClass().getSimpleName(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get connection for saving PlayerData", e);
        }
    }
}
