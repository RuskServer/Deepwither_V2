package com.ruskserver.deepwither_V2.modules.dungeon.generator;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * 空のチャンクを生成する超軽量ジェネレータ。
 * <p>
 * 単一の固定 {@link BiomeProvider} と固定スポーン座標を返すことで、
 * Paper 1.21+ / 26.3 でのバイオームノイズ計算およびスポーン検索によるチャンク読み込みを排除します。
 */
public class VoidChunkGenerator extends ChunkGenerator {

    private static final BiomeProvider SINGLE_BIOME_PROVIDER = new BiomeProvider() {
        private final List<Biome> biomes = List.of(Biome.PLAINS);

        @Override
        public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            return Biome.PLAINS;
        }

        @Override
        public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return biomes;
        }
    };

    public VoidChunkGenerator() {
        super();
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return SINGLE_BIOME_PROVIDER;
    }

    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0.5, 64.0, 0.5);
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
