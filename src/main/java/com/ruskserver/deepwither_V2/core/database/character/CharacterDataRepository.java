package com.ruskserver.deepwither_V2.core.database.character;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ruskserver.deepwither_V2.core.database.DatabaseManager;
import com.ruskserver.deepwither_V2.core.database.player.DataKey;
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
public class CharacterDataRepository extends CachedRepository<UUID, CharacterData> {

    private final Logger logger;
    private final DIContainer container;
    private final List<CharacterDataProvider<?>> providers = new ArrayList<>();
    private boolean providersLoaded = false;

    @Inject
    public CharacterDataRepository(DatabaseManager db, DIContainer container) {
        super(db);
        this.logger = Logger.getLogger("CharacterDataRepo");
        this.container = container;
    }

    private void ensureProvidersLoaded() {
        if (providersLoaded) return;
        providersLoaded = true;

        for (Object instance : container.getAllInstances()) {
            if (instance instanceof CharacterDataProvider) {
                providers.add((CharacterDataProvider<?>) instance);
                logger.info("Registered CharacterDataProvider: " + instance.getClass().getSimpleName());
            }
        }
    }

    @Override
    protected Cache<UUID, CharacterData> buildCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
    }

    @Override
    protected Optional<CharacterData> loadFromDb(UUID key) {
        ensureProvidersLoaded();
        CharacterData characterData = new CharacterData(key);

        try (Connection conn = db.getConnection()) {
            for (CharacterDataProvider<?> provider : providers) {
                try {
                    Object data = provider.loadFromDb(key, conn);
                    if (data != null) {
                        @SuppressWarnings("unchecked")
                        CharacterDataProvider<Object> objProvider = (CharacterDataProvider<Object>) provider;
                        characterData.setInitial(objProvider.getKey(), data);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load data from provider: " + provider.getClass().getSimpleName(), e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get connection for loading CharacterData", e);
        }

        return Optional.of(characterData);
    }

    @Override
    protected void saveToDb(UUID key, CharacterData value) {
        ensureProvidersLoaded();
        Set<DataKey<?>> dirtyKeys = value.getDirtyKeys();

        if (dirtyKeys.isEmpty()) {
            return;
        }

        try (Connection conn = db.getConnection()) {
            for (CharacterDataProvider<?> provider : providers) {
                if (dirtyKeys.contains(provider.getKey())) {
                    try {
                        @SuppressWarnings("unchecked")
                        CharacterDataProvider<Object> objProvider = (CharacterDataProvider<Object>) provider;
                        Object data = value.get(objProvider.getKey());
                        
                        objProvider.saveToDb(key, data, conn);
                        
                        value.clearDirtyFlag(objProvider.getKey());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to save data from provider: " + provider.getClass().getSimpleName(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get connection for saving CharacterData", e);
        }
    }
}
