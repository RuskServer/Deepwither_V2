package com.ruskserver.deepwither_V2.modules.lootchest.api;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerChestData {
    private final UUID playerUuid;
    private final UUID chestId;
    private LocalDateTime nextOpenTime;

    public PlayerChestData(UUID playerUuid, UUID chestId, LocalDateTime nextOpenTime) {
        this.playerUuid = playerUuid;
        this.chestId = chestId;
        this.nextOpenTime = nextOpenTime;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public UUID getChestId() { return chestId; }
    public LocalDateTime getNextOpenTime() { return nextOpenTime; }
    public void setNextOpenTime(LocalDateTime nextOpenTime) { this.nextOpenTime = nextOpenTime; }
}
