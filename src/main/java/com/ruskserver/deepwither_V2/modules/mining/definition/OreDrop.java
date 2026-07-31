package com.ruskserver.deepwither_V2.modules.mining.definition;

import org.bukkit.Material;

public record OreDrop(Material material, String customItemId, int minAmount, int maxAmount, double chance) {

    public OreDrop {
        if (material == null && (customItemId == null || customItemId.isBlank())) {
            throw new IllegalArgumentException("material or customItemId is required");
        }
        minAmount = Math.max(1, minAmount);
        maxAmount = Math.max(minAmount, maxAmount);
        chance = Math.max(0.0D, Math.min(1.0D, chance));
    }

    public static OreDrop vanilla(Material material, int amount) {
        return new OreDrop(material, null, amount, amount, 1.0D);
    }

    public static OreDrop custom(String customItemId, int amount) {
        return new OreDrop(null, customItemId, amount, amount, 1.0D);
    }

    public static OreDrop custom(String customItemId, int minAmount, int maxAmount, double chance) {
        return new OreDrop(null, customItemId, minAmount, maxAmount, chance);
    }
}
