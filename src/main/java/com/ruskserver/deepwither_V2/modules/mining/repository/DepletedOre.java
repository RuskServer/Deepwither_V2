package com.ruskserver.deepwither_V2.modules.mining.repository;

import org.bukkit.Material;

import java.util.UUID;

public record DepletedOre(
        UUID worldId,
        int x,
        int y,
        int z,
        Material material,
        long respawnAtMillis) {
}
