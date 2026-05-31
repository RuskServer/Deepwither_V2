package com.ruskserver.deepwither_V2.modules.lootchest.api;

import org.bukkit.Location;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 設置されたルートチェストの情報を保持するデータモデル。
 */
public class LootChestLocation {
    private final UUID id;
    private final Location location;
    private final String lootTableId;
    private LocalDateTime nextSpawnTime;
    private boolean spawned;

    public LootChestLocation(UUID id, Location location, String lootTableId, LocalDateTime nextSpawnTime, boolean spawned) {
        this.id = id;
        this.location = location;
        this.lootTableId = lootTableId;
        this.nextSpawnTime = nextSpawnTime;
        this.spawned = spawned;
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public String getLootTableId() {
        return lootTableId;
    }

    public LocalDateTime getNextSpawnTime() {
        return nextSpawnTime;
    }

    public void setNextSpawnTime(LocalDateTime nextSpawnTime) {
        this.nextSpawnTime = nextSpawnTime;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public void setSpawned(boolean spawned) {
        this.spawned = spawned;
    }
}
