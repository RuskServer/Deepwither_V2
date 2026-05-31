package com.ruskserver.deepwither_V2.modules.lootchest.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootItem;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootTableDefinition;

import java.util.Arrays;
import java.util.List;

@Component
public class GhoulNestLootTable implements LootTableDefinition {

    private final ItemManager itemManager;

    @Inject
    public GhoulNestLootTable(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @Override
    public String getId() {
        return "ghoul_nest";
    }

    @Override
    public String getDisplayName() {
        return "グールの巣のチェスト";
    }

    @Override
    public List<LootItem> getLootItems() {
        return Arrays.asList(
                // 新規素材
                new LootItem(itemManager.generate("moonlight_residue"), 100, 1, 5),
                new LootItem(itemManager.generate("abyss_shard"), 50, 1, 3),
                new LootItem(itemManager.generate("void_star_dust"), 15, 1, 1),
                
                // グール素材
                new LootItem(itemManager.generate("ghoul_viscera"), 80, 2, 4),
                new LootItem(itemManager.generate("ghoul_remnant"), 40, 1, 2),
                new LootItem(itemManager.generate("ghoul_essence"), 20, 1, 1),
                
                // アーティファクト
                new LootItem(itemManager.generate("artifact_box_item"), 10, 1, 1)
        );
    }

    @Override
    public int getRolls() {
        return 6;
    }
}
