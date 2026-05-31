package com.ruskserver.deepwither_V2.modules.lootchest.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootItem;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootTableDefinition;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

@Component
public class TestLootTable implements LootTableDefinition {
    @Override
    public String getId() {
        return "test_loot";
    }

    @Override
    public String getDisplayName() {
        return "テストルートチェスト";
    }

    @Override
    public List<LootItem> getLootItems() {
        return Arrays.asList(
                new LootItem(new ItemStack(Material.DIAMOND), 10, 1, 3),
                new LootItem(new ItemStack(Material.GOLD_INGOT), 30, 2, 8),
                new LootItem(new ItemStack(Material.IRON_INGOT), 60, 5, 16)
        );
    }

    @Override
    public int getRolls() {
        return 5;
    }
}
