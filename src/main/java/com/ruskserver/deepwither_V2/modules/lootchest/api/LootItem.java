package com.ruskserver.deepwither_V2.modules.lootchest.api;

import org.bukkit.inventory.ItemStack;

/**
 * ルートチェストに含まれるアイテムの定義。
 */
public record LootItem(ItemStack itemStack, int weight, int minAmount, int maxAmount) {
}
