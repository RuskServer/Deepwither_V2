package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.lootchest.service.LootChestManager;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * ダンジョン内の報酬配置を管理するサービス。
 * <p>
 * DIAMOND_BLOCK マーカーの位置にチェストを設置します。
 */
@Service
public class LootService {

    private static final String DEFAULT_DUNGEON_LOOT_TABLE = "ghoul_nest";

    private final LootChestManager lootChestManager;
    private final Logger log;

    @Inject
    public LootService(LootChestManager lootChestManager, Logger log) {
        this.lootChestManager = lootChestManager;
        this.log = log;
    }

    /**
     * 指定されたワールドにチェストを設置します。
     *
     * @param world         対象ワールド
     * @param lootPositions 報酬位置（ワールド座標）
     * @param lootTableId   使用するルートテーブルID（null または空の場合はデフォルト）
     * @return 設置したチェストの数
     */
    public int placeChests(World world, List<BlockVector3> lootPositions, String lootTableId) {
        String effectiveLootTable = (lootTableId != null && !lootTableId.isBlank()) ? lootTableId : DEFAULT_DUNGEON_LOOT_TABLE;
        int placed = 0;
        for (BlockVector3 pos : lootPositions) {
            lootChestManager.placeOneShotChest(new Location(world, pos.x(), pos.y(), pos.z()), effectiveLootTable);
            placed++;
            log.fine("[LootService] チェスト設置: (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")");
        }

        log.info("[LootService] " + placed + " 個のチェストを設置しました (lootTable=" + effectiveLootTable + ")");
        return placed;
    }

    /**
     * デフォルトのルートテーブルでチェストを設置します。
     */
    public int placeChests(World world, List<BlockVector3> lootPositions) {
        return placeChests(world, lootPositions, null);
    }

    /**
     * 指定されたチェストにアイテムを配置します。
     *
     * @param world  対象ワールド
     * @param pos    チェストの位置
     * @param items  配置するアイテム
     */
    public void fillChest(World world, BlockVector3 pos, List<ItemStack> items) {
        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
        if (block.getState() instanceof Chest chest) {
            chest.getInventory().clear();
            for (ItemStack item : items) {
                Map<Integer, ItemStack> leftover = chest.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    log.warning("[LootService] チェスト容量不足: " + leftover.size() + " 個のアイテムを格納できませんでした");
                }
            }
            chest.update();
        }
    }
}
