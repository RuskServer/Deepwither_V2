package com.ruskserver.deepwither_V2.modules.dungeon.schematic;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorDirection;
import com.ruskserver.deepwither_V2.modules.dungeon.room.DoorType;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * WorldEdit の .schem ファイルを読み込み、マーカーブロックを解析して {@link RoomSchematic} を生成するサービス。
 * <p>
 * マーカーブロック仕様:
 * <ul>
 *   <li>{@link Material#IRON_BLOCK} - ドア入口（ENTRY）。隣接する空気ブロックの方向が開口方向。</li>
 *   <li>{@link Material#GOLD_BLOCK} - ドア出口（EXIT）。隣接する空気ブロックの方向が開口方向。</li>
 *   <li>{@link Material#EMERALD_BLOCK} - モブスポーンポイント。</li>
 *   <li>{@link Material#DIAMOND_BLOCK} - 報酬配置位置。</li>
 *   <li>{@link Material#LAPIS_BLOCK} - エントリーテレポート地点。</li>
 *   <li>{@link Material#REDSTONE_BLOCK} - ボススポーン地点。</li>
 * </ul>
 * マーカーブロックは読み込み後に除去されます（配置後にワールドに残りません）。
 */
@Service
public class SchematicLoader implements Startable {

    private final JavaPlugin plugin;
    private final Logger log;

    @Inject
    public SchematicLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    @Override
    public void start() {
        File dungeonFolder = new File(plugin.getDataFolder(), "dungeon");
        if (!dungeonFolder.exists()) {
            dungeonFolder.mkdirs();
            log.info("[SchematicLoader] dungeon フォルダを作成しました: " + dungeonFolder.getAbsolutePath());
        }

        // テスト用スケマティックを自動生成（既に存在する場合はスキップ）
        SchematicGenerator generator = new SchematicGenerator(plugin);
        generator.generateAllForDungeon("eternal_ice");
        log.info("[SchematicLoader] テスト用スケマティックの自動生成を完了しました。");
    }

    /**
     * 指定パスの .schem ファイルを読み込み、マーカーブロックを解析して {@link RoomSchematic} を返します。
     *
     * @param schematicFile  .schem ファイルのパス
     * @param schematicId    スケマティック識別子（"ダンジョンID:schematicName"）
     * @return 読み込み済みルームデータ。読み込み失敗場合は null
     */
    public RoomSchematic load(File schematicFile, String schematicId) {
        if (!schematicFile.exists()) {
            log.warning("[SchematicLoader] スケマティックファイルが見つかりません: " + schematicFile.getAbsolutePath());
            return null;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            log.warning("[SchematicLoader] サポートされていない形式です: " + schematicFile.getName());
            return null;
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            Clipboard clipboardBase = reader.read();
            if (!(clipboardBase instanceof BlockArrayClipboard clipboard)) {
                log.warning("[SchematicLoader] BlockArrayClipboard に変換できません: " + schematicFile.getName());
                return null;
            }

            BlockVector3 min = clipboard.getMinimumPoint();
            BlockVector3 max = clipboard.getMaximumPoint();
            clipboard.setOrigin(min);

            List<DoorDefinition> entryDoors = new ArrayList<>();
            List<DoorDefinition> exitDoors = new ArrayList<>();
            List<BlockVector3> mobSpawns = new ArrayList<>();
            List<BlockVector3> lootPositions = new ArrayList<>();
            BlockVector3 entryTeleport = null;
            BlockVector3 bossSpawn = null;

            for (int x = min.x(); x <= max.x(); x++) {
                for (int y = min.y(); y <= max.y(); y++) {
                    for (int z = min.z(); z <= max.z(); z++) {
                        BlockVector3 pos = BlockVector3.at(x, y, z);
                        BlockState blockState = clipboard.getBlock(pos);
                        Material material = toBukkitMaterial(blockState);

                        if (material == null) continue;

                        switch (material) {
                            case IRON_BLOCK -> {
                                DoorDirection dir = detectDoorDirection(clipboard, pos);
                                entryDoors.add(new DoorDefinition(toLocal(pos, min), dir, DoorType.ENTRY, List.of()));
                            }
                            case GOLD_BLOCK -> {
                                DoorDirection dir = detectDoorDirection(clipboard, pos);
                                exitDoors.add(new DoorDefinition(toLocal(pos, min), dir, DoorType.EXIT, List.of()));
                            }
                            case EMERALD_BLOCK -> mobSpawns.add(toLocal(pos, min));
                            case DIAMOND_BLOCK -> lootPositions.add(toLocal(pos, min));
                            case LAPIS_BLOCK -> entryTeleport = toLocal(pos, min);
                            case REDSTONE_BLOCK -> bossSpawn = toLocal(pos, min);
                        }
                    }
                }
            }

            // マーカーブロックを除去
            removeMarkers(clipboard, min, max);

            BlockVector3 origin = entryTeleport != null ? entryTeleport : BlockVector3.ZERO;

            log.info("[SchematicLoader] " + schematicId + ": size=" + clipboard.getDimensions()
                    + ", entryDoors=" + entryDoors
                    + ", exitDoors=" + exitDoors
                    + ", mobSpawns=" + mobSpawns.size());

            return new RoomSchematic(
                    schematicId,
                    clipboard,
                    entryDoors,
                    exitDoors,
                    mobSpawns,
                    lootPositions,
                    entryTeleport,
                    bossSpawn,
                    origin
            );

        } catch (IOException e) {
            log.severe("[SchematicLoader] スケマティック読み込み失敗: " + schematicFile.getName() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * ダンジョンフォルダ内の .schem ファイルを読み込みます。
     */
    public RoomSchematic loadFromDungeonFolder(String dungeonFolder, String dungeonId, String schematicName) {
        File file = new File(plugin.getDataFolder(), "dungeon/" + dungeonFolder + "/" + schematicName + ".schem");
        String id = dungeonId + ":" + schematicName;
        return load(file, id);
    }

    /**
     * マーカーブロックの方向を検出します。
     * 4方向をスキャンし、最初に見つかった空気ブロックの方向を返します。
     */
    private DoorDirection detectDoorDirection(BlockArrayClipboard clipboard, BlockVector3 pos) {
        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();

        // 1. 範囲外を開口方向（部屋の外側）とみなすのを最優先する
        for (DoorDirection dir : DoorDirection.values()) {
            BlockVector3 adjacent = pos.add(dir.getOffset());
            if (!isInBounds(adjacent, min, max)) {
                return dir;
            }
        }

        // 2. 全て範囲内の場合、範囲内の空気ブロックを検出（開口方向）
        for (DoorDirection dir : DoorDirection.values()) {
            BlockVector3 adjacent = pos.add(dir.getOffset());
            if (isInBounds(adjacent, min, max) && isAir(clipboard, adjacent)) {
                return dir;
            }
        }
        return DoorDirection.NORTH;
    }

    private static boolean isInBounds(BlockVector3 pos, BlockVector3 min, BlockVector3 max) {
        return pos.x() >= min.x() && pos.x() <= max.x()
                && pos.y() >= min.y() && pos.y() <= max.y()
                && pos.z() >= min.z() && pos.z() <= max.z();
    }

    /**
     * 指定座標が空気ブロックか判定します。
     * クリップボードの範囲外は空気とみなします。
     */
    private boolean isAir(BlockArrayClipboard clipboard, BlockVector3 pos) {
        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();
        if (pos.x() < min.x() || pos.x() > max.x()
                || pos.y() < min.y() || pos.y() > max.y()
                || pos.z() < min.z() || pos.z() > max.z()) {
            return true;
        }
        BlockState blockState = clipboard.getBlock(pos);
        Material material = toBukkitMaterial(blockState);
        return material == null || material.isAir();
    }

    /**
     * マーカーブロックをクリップボードから除去します（空気に置き換え）。
     */
    private void removeMarkers(BlockArrayClipboard clipboard, BlockVector3 min, BlockVector3 max) {
        BlockState airState = BlockTypes.AIR.getDefaultState();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    BlockState blockState = clipboard.getBlock(pos);
                    Material material = toBukkitMaterial(blockState);
                    if (material != null && isMarkerBlock(material)) {
                        clipboard.setBlock(pos, airState);
                    }
                }
            }
        }
    }

    private BlockVector3 toLocal(BlockVector3 pos, BlockVector3 origin) {
        return pos.subtract(origin);
    }

    /**
     * マーカーブロックかどうかを判定します。
     */
    private boolean isMarkerBlock(Material material) {
        return switch (material) {
            case IRON_BLOCK, GOLD_BLOCK, EMERALD_BLOCK, DIAMOND_BLOCK, LAPIS_BLOCK, REDSTONE_BLOCK -> true;
            default -> false;
        };
    }

    /**
     * WorldEdit の BlockState を Bukkit Material に変換します。
     */
    private Material toBukkitMaterial(BlockState blockState) {
        BlockType weType = blockState.getBlockType();
        // BlockType#id() は "minecraft:stone" 形式のIDを返す
        String id = weType.id();
        String materialName = id.replace("minecraft:", "").toUpperCase(java.util.Locale.ROOT);
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
