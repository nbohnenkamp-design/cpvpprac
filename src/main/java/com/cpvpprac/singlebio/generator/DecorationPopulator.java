package com.cpvpprac.singlebio.generator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class DecorationPopulator extends BlockPopulator {

    private final SingleBiomeChunkGenerator.BiomeType biomeType;
    private final double grassDensity;
    private final double flowerDensity;
    private final double treeDensity;
    private final double cactusDensity;
    private final double deadBushDensity;
    private final double mushroomDensity;

    // Flower materials for plains
    private static final Material[] PLAINS_FLOWERS = {
            Material.POPPY, Material.DANDELION, Material.AZURE_BLUET,
            Material.OXEYE_DAISY, Material.CORNFLOWER
    };

    public DecorationPopulator(SingleBiomeChunkGenerator.BiomeType biomeType,
                               double grassDensity, double flowerDensity, double treeDensity,
                               double cactusDensity, double deadBushDensity, double mushroomDensity) {
        this.biomeType     = biomeType;
        this.grassDensity  = grassDensity;
        this.flowerDensity = flowerDensity;
        this.treeDensity   = treeDensity;
        this.cactusDensity = cactusDensity;
        this.deadBushDensity = deadBushDensity;
        this.mushroomDensity = mushroomDensity;
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        switch (biomeType) {
            case PLAINS -> populatePlains(random, chunkX, chunkZ, region, worldInfo);
            case DESERT -> populateDesert(random, chunkX, chunkZ, region, worldInfo);
            case MUSHROOM -> populateMushroom(random, chunkX, chunkZ, region, worldInfo);
            default -> { /* no decoration for snow, badlands, end */ }
        }
    }

    private void populatePlains(Random random, int chunkX, int chunkZ, LimitedRegion region, WorldInfo info) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkX * 16 + x;
                int wz = chunkZ * 16 + z;
                int surface = getSurfaceY(region, wx, wz, info);
                if (surface < 0) continue;

                if (random.nextDouble() < grassDensity) {
                    placePlant(region, wx, surface + 1, wz, Material.SHORT_GRASS);
                } else if (random.nextDouble() < flowerDensity) {
                    Material flower = PLAINS_FLOWERS[random.nextInt(PLAINS_FLOWERS.length)];
                    placePlant(region, wx, surface + 1, wz, flower);
                } else if (random.nextDouble() < treeDensity) {
                    placeOakTree(region, wx, surface + 1, wz, random, info);
                }
            }
        }
    }

    private void populateDesert(Random random, int chunkX, int chunkZ, LimitedRegion region, WorldInfo info) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkX * 16 + x;
                int wz = chunkZ * 16 + z;
                int surface = getSurfaceY(region, wx, wz, info);
                if (surface < 0) continue;

                if (random.nextDouble() < cactusDensity) {
                    placeCactus(region, wx, surface + 1, wz, random, info);
                } else if (random.nextDouble() < deadBushDensity) {
                    placePlant(region, wx, surface + 1, wz, Material.DEAD_BUSH);
                }
            }
        }
    }

    private void populateMushroom(Random random, int chunkX, int chunkZ, LimitedRegion region, WorldInfo info) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkX * 16 + x;
                int wz = chunkZ * 16 + z;
                int surface = getSurfaceY(region, wx, wz, info);
                if (surface < 0) continue;

                if (random.nextDouble() < mushroomDensity) {
                    Material m = random.nextBoolean() ? Material.BROWN_MUSHROOM : Material.RED_MUSHROOM;
                    placePlant(region, wx, surface + 1, wz, m);
                }
            }
        }
    }

    private int getSurfaceY(LimitedRegion region, int x, int z, WorldInfo info) {
        if (!region.isInRegion(x, info.getMinHeight(), z)) return -1;
        for (int y = info.getMaxHeight() - 1; y > info.getMinHeight(); y--) {
            if (!region.isInRegion(x, y, z)) continue;
            Material mat = region.getType(x, y, z);
            if (mat != Material.AIR && mat != Material.CAVE_AIR) return y;
        }
        return -1;
    }

    private void placePlant(LimitedRegion region, int x, int y, int z, Material mat) {
        if (!region.isInRegion(x, y, z)) return;
        if (region.getType(x, y, z) == Material.AIR) {
            region.setType(x, y, z, mat);
        }
    }

    private void placeCactus(LimitedRegion region, int x, int y, int z, Random random, WorldInfo info) {
        int height = 1 + random.nextInt(3);
        for (int i = 0; i < height; i++) {
            if (y + i >= info.getMaxHeight()) break;
            if (!region.isInRegion(x, y + i, z)) break;
            if (region.getType(x, y + i, z) != Material.AIR) break;
            region.setType(x, y + i, z, Material.CACTUS);
        }
    }

    private void placeOakTree(LimitedRegion region, int x, int y, int z, Random random, WorldInfo info) {
        int trunkHeight = 4 + random.nextInt(3);

        // Check space
        for (int i = 0; i < trunkHeight + 1; i++) {
            if (y + i >= info.getMaxHeight()) return;
            if (!region.isInRegion(x, y + i, z)) return;
        }

        // Trunk
        for (int i = 0; i < trunkHeight; i++) {
            if (!region.isInRegion(x, y + i, z)) break;
            region.setType(x, y + i, z, Material.OAK_LOG);
        }

        Leaves leafData = (Leaves) Bukkit.createBlockData(Material.OAK_LEAVES);
        leafData.setPersistent(true);

        // Leaves: 2-layer canopy at top
        int leafBase = y + trunkHeight - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    int lx = x + dx, ly = leafBase + dy, lz = z + dz;
                    if (!region.isInRegion(lx, ly, lz)) continue;
                    if (region.getType(lx, ly, lz) == Material.AIR) {
                        region.setBlockData(lx, ly, lz, leafData);
                    }
                }
            }
        }
        // Top two
        if (region.isInRegion(x, leafBase + 2, z))
            region.setBlockData(x, leafBase + 2, z, leafData);
        if (region.isInRegion(x, leafBase + 3, z))
            region.setBlockData(x, leafBase + 3, z, leafData);
    }
}
