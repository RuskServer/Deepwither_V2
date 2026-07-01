package com.ruskserver.deepwither_V2.modules.dungeon.schematic;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * テスト用のスケマティックファイルを自動生成するユーティリティ。
 * <p>
 * ダンジョン起動時にスケマティックが存在しない場合に限り自動生成します。
 * 本番運用では手動で作成したスケマティックに差し替えてください。
 */
public class SchematicGenerator {

    private final JavaPlugin plugin;
    private final Logger log;

    public SchematicGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    /**
     * 指定ダンジョンの全スケマティックを生成します（既に存在する場合はスキップ）。
     */
    public void generateAllForDungeon(String dungeonFolder) {
        File folder = new File(plugin.getDataFolder(), "dungeon/" + dungeonFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        generateStraightCorridor(dungeonFolder);
        generateSmallRoom(dungeonFolder);
    }

    /**
     * 直線廊下を生成します。
     * サイズ: 5x5x11 (x, y, z)
     * マーカー: 北側に IRON_BLOCK (入口)、南側に GOLD_BLOCK (出口)
     */
    public void generateStraightCorridor(String dungeonFolder) {
        File file = getFile(dungeonFolder, "corridor_straight");
        if (file.exists()) return;

        int width = 5, height = 5, length = 11;
        BlockArrayClipboard clipboard = new BlockArrayClipboard(
                new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(width - 1, height - 1, length - 1))
        );

        BlockState stone = BlockTypes.STONE.getDefaultState();
        BlockState air = BlockTypes.AIR.getDefaultState();
        BlockState iron = BlockTypes.IRON_BLOCK.getDefaultState();
        BlockState gold = BlockTypes.GOLD_BLOCK.getDefaultState();

        // 外殻
        buildBox(clipboard, width, height, length, stone);
        // 内部を空気に
        clearInterior(clipboard, width, height, length);

        // 北ドア入口 (z=0)
        clipboard.setBlock(BlockVector3.at(2, 1, 0), iron);
        // 南ドア出口 (z=10)
        clipboard.setBlock(BlockVector3.at(2, 1, length - 1), gold);

        save(clipboard, file);
    }

    /**
     * 小部屋を生成します。
     * サイズ: 7x5x7
     * マーカー: 南側に IRON_BLOCK (入口)、中央に LAPIS_BLOCK (テレポート地点)
     */
    public void generateSmallRoom(String dungeonFolder) {
        File file = getFile(dungeonFolder, "room_small");
        if (file.exists()) return;

        int size = 7;
        BlockArrayClipboard clipboard = new BlockArrayClipboard(
                new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(size - 1, 4, size - 1))
        );

        BlockState stone = BlockTypes.STONE.getDefaultState();
        BlockState iron = BlockTypes.IRON_BLOCK.getDefaultState();
        BlockState lapis = BlockTypes.LAPIS_BLOCK.getDefaultState();

        // 外殻
        buildBox(clipboard, size, 5, size, stone);
        // 内部を空気に
        clearInterior(clipboard, size, 5, size);

        // 南ドア入口 (z=0)
        clipboard.setBlock(BlockVector3.at(3, 1, 0), iron);
        // ラピス (入口テレポート地点)
        clipboard.setBlock(BlockVector3.at(3, 1, 3), lapis);

        save(clipboard, file);
    }

    // --- ヘルパー ---

    private void buildBox(BlockArrayClipboard clipboard, int w, int h, int d, BlockState block) {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    if (x == 0 || x == w - 1 || y == 0 || y == h - 1 || z == 0 || z == d - 1) {
                        clipboard.setBlock(BlockVector3.at(x, y, z), block);
                    }
                }
            }
        }
    }

    private void clearInterior(BlockArrayClipboard clipboard, int w, int h, int d) {
        BlockState air = BlockTypes.AIR.getDefaultState();
        for (int x = 1; x < w - 1; x++) {
            for (int y = 1; y < h - 1; y++) {
                for (int z = 1; z < d - 1; z++) {
                    clipboard.setBlock(BlockVector3.at(x, y, z), air);
                }
            }
        }
    }

    private File getFile(String dungeonFolder, String name) {
        File folder = new File(plugin.getDataFolder(), "dungeon/" + dungeonFolder);
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, name + ".schem");
    }

    private void save(BlockArrayClipboard clipboard, File file) {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            format = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC;
        }

        try (ClipboardWriter writer = format.getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
            log.fine("[SchematicGenerator] 生成: " + file.getName());
        } catch (IOException e) {
            log.warning("[SchematicGenerator] 生成失敗: " + file.getName() + " - " + e.getMessage());
        }
    }
}
