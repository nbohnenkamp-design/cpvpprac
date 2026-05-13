package com.cpvpprac.singlebio.generator;

import com.cpvpprac.singlebio.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.List;
import java.util.Random;

public class SingleBiomeChunkGenerator extends ChunkGenerator {

    public enum BiomeType {
        PLAINS, DESERT, BADLANDS, SNOW, MUSHROOM, END;

        public static BiomeType fromKey(String key) {
            return switch (key) {
                case "desert"   -> DESERT;
                case "badlands" -> BADLANDS;
                case "snow"     -> SNOW;
                case "mushroom" -> MUSHROOM;
                case "end"      -> END;
                default         -> PLAINS;
            };
        }

        public String configKey() {
            return name().toLowerCase();
        }
    }

    private final BiomeType biomeType;
    private final int baseHeight;
    private final double flatness;
    private final int heightVariation;
    private final double noiseScale;
    private final int noiseOctaves;
    private final boolean decorationsEnabled;
    private final double grassDensity;
    private final double flowerDensity;
    private final double treeDensity;
    private final double cactusDensity;
    private final double deadBushDensity;
    private final double mushroomDensity;

    private SimplexOctaveGenerator noiseGenerator;

    public SingleBiomeChunkGenerator(ConfigManager config, BiomeType biomeType) {
        this.biomeType = biomeType;
        String key = biomeType.configKey();
        this.baseHeight      = config.getBaseHeight(key);
        this.flatness        = config.getFlatness(key);
        this.heightVariation = config.getHeightVariation(key);
        this.noiseScale      = config.getNoiseScale(key);
        this.noiseOctaves    = config.getNoiseOctaves(key);
        this.decorationsEnabled = config.isDecorationsEnabled(key);
        this.grassDensity    = config.getGrassDensity(key);
        this.flowerDensity   = config.getFlowerDensity(key);
        this.treeDensity     = config.getTreeDensity(key);
        this.cactusDensity   = config.getCactusDensity(key);
        this.deadBushDensity = config.getDeadBushDensity(key);
        this.mushroomDensity = config.getMushroomDensity(key);
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunk) {
        SimplexOctaveGenerator noise = getOrCreateNoise(worldInfo.getSeed());
        int minY = worldInfo.getMinHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkX * 16 + x;
                int wz = chunkZ * 16 + z;

                double n = noise.noise(wx, wz, 0.5, 0.5, true);
                int surfaceY = baseHeight + (int) (n * heightVariation * (1.0 - flatness));

                chunk.setBlock(x, minY, z, Material.BEDROCK);

                for (int y = minY + 1; y < surfaceY - 3; y++) {
                    chunk.setBlock(x, y, z, getStoneBlock());
                }

                int subStart = Math.max(minY + 1, surfaceY - 3);
                for (int y = subStart; y < surfaceY; y++) {
                    chunk.setBlock(x, y, z, getSubsurfaceBlock());
                }

                chunk.setBlock(x, surfaceY, z, getSurfaceBlock());
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new SingleBiomeProvider(getBukkitBiome());
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(org.bukkit.World world) {
        if (!decorationsEnabled) return List.of();
        return List.of(new DecorationPopulator(
                biomeType, grassDensity, flowerDensity, treeDensity,
                cactusDensity, deadBushDensity, mushroomDensity));
    }

    // No vanilla generation passes
    @Override public boolean shouldGenerateNoise()        { return false; }
    @Override public boolean shouldGenerateCaves()        { return false; }
    @Override public boolean shouldGenerateDecorations()  { return false; }
    @Override public boolean shouldGenerateMobs()         { return false; }
    @Override public boolean shouldGenerateStructures()   { return false; }

    private synchronized SimplexOctaveGenerator getOrCreateNoise(long seed) {
        if (noiseGenerator == null) {
            noiseGenerator = new SimplexOctaveGenerator(seed, noiseOctaves);
            noiseGenerator.setScale(noiseScale);
        }
        return noiseGenerator;
    }

    private Material getSurfaceBlock() {
        return switch (biomeType) {
            case PLAINS   -> Material.GRASS_BLOCK;
            case DESERT   -> Material.SAND;
            case BADLANDS -> Material.RED_SAND;
            case SNOW     -> Material.GRASS_BLOCK;
            case MUSHROOM -> Material.MYCELIUM;
            case END      -> Material.END_STONE;
        };
    }

    private Material getSubsurfaceBlock() {
        return switch (biomeType) {
            case PLAINS, SNOW, MUSHROOM -> Material.DIRT;
            case DESERT                 -> Material.SANDSTONE;
            case BADLANDS               -> Material.TERRACOTTA;
            case END                    -> Material.END_STONE;
        };
    }

    private Material getStoneBlock() {
        return switch (biomeType) {
            case END -> Material.END_STONE;
            default  -> Material.STONE;
        };
    }

    private Biome getBukkitBiome() {
        return switch (biomeType) {
            case PLAINS   -> Biome.PLAINS;
            case DESERT   -> Biome.DESERT;
            case BADLANDS -> Biome.BADLANDS;
            case SNOW     -> Biome.SNOWY_PLAINS;
            case MUSHROOM -> Biome.MUSHROOM_FIELDS;
            case END      -> Biome.THE_END;
        };
    }

    // Inner BiomeProvider — single biome for the whole world
    private static class SingleBiomeProvider extends BiomeProvider {
        private final Biome biome;

        SingleBiomeProvider(Biome biome) {
            this.biome = biome;
        }

        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            return biome;
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return List.of(biome);
        }
    }
}
