package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import org.bukkit.generator.ChunkGenerator;

/**
 * 空のチャンクを生成するジェネレータ。
 * <p>
 * ダンジョン用の void ワールドで使用します。
 */
public class VoidChunkGenerator extends ChunkGenerator {

    public VoidChunkGenerator() {
        super();
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
