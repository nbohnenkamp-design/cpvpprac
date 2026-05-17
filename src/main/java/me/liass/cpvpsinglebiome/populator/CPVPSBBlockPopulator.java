package me.liass.cpvpsinglebiome.populator;

import java.util.Random;

import me.liass.cpvpsinglebiome.config.ConfigManager;
import me.liass.cpvpsinglebiome.generator.BiomeType;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

public class CPVPSBBlockPopulator extends BlockPopulator {

    private static final Material[] PLAINS_FLOWERS = new Material[] {
            Material.POPPY,
            Material.DANDELION,
            Material.OXEYE_DAISY,
            Material.AZURE_BLUET,
            Material.CORNFLOWER,
            Material.BLUE_ORCHID
    };

    private static final Material[] BADLANDS_COLORS = new Material[] {
            Material.TERRACOTTA,
            Material.ORANGE_TERRACOTTA,
            Material.YELLOW_TERRACOTTA,
            Material.RED_TERRACOTTA,
            Material.BROWN_TERRACOTTA,
            Material.WHITE_TERRACOTTA
    };

    private final ConfigManager config;
    private final BiomeType biomeType;

    public CPVPSBBlockPopulator(
            ConfigManager config,
            BiomeType biomeType
    ) {
        this.config = config;
        this.biomeType = biomeType;
    }

    @Override
    public void populate(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            LimitedRegion region
    ) {
        if (!this.config.isDecorationEnabled()) {
            return;
        }

        double density =
                this.config.getBiomeDecorationDensity(
                        this.biomeType.getId()
                );

        if (density <= 0.0D) {
            return;
        }

        switch (this.biomeType) {

            case PLAINS:
                populatePlains(random, chunkX, chunkZ, region, density);
                break;

            case DESERT:
                populateDesert(random, chunkX, chunkZ, region, density);
                break;

            case BADLANDS:
                populateBadlands(random, chunkX, chunkZ, region, density);
                break;

            case SNOW:
                populateSnow(random, chunkX, chunkZ, region, density);
                break;

            case MUSHROOM:
                populateMushroom(random, chunkX, chunkZ, region, density);
                break;

            case END:
                populateEnd(random, chunkX, chunkZ, region, density);
                break;
        }
    }

    private void populatePlains(
            Random r,
            int cx,
            int cz,
            LimitedRegion region,
            double density
    ) {
        if (this.config.isFeatureEnabled("flowers")) {
            int grass = featureCount(r, density * 10.0D);

            for (int i = 0; i < grass; i++) {
                placeOnGrass(region, r, cx, cz, Material.SHORT_GRASS);
            }

            int flowers = featureCount(r, density * 2.0D);

            for (int i = 0; i < flowers; i++) {
                placeOnGrass(
                        region,
                        r,
                        cx,
                        cz,
                        PLAINS_FLOWERS[r.nextInt(PLAINS_FLOWERS.length)]
                );
            }
        }

        if (this.config.isFeatureEnabled("small-trees")) {
            int bushes = featureCount(
                    r,
                    density
                            * this.config.getBiomeTreeDensity(
                                    this.biomeType.getId()
                            )
                            * 0.35D
            );

            for (int i = 0; i < bushes; i++) {
                int x = (cx << 4) + 4 + r.nextInt(8);
                int z = (cz << 4) + 4 + r.nextInt(8);

                int y = findTopBlockY(region, x, z);

                if (y < 0) {
                    continue;
                }

                if (region.getType(x, y, z) != Material.GRASS_BLOCK) {
                    continue;
                }

                if (!hasAir(region, x, y + 1, z)) {
                    continue;
                }

                placeSmallOakBush(region, x, y + 1, z);
            }
        }
    }

    private void populateDesert(
            Random r,
            int cx,
            int cz,
            LimitedRegion region,
            double density
    ) {
        if (this.config.isFeatureEnabled("dead-bushes")) {
            int deadBushes = featureCount(r, density * 1.2D);

            for (int i = 0; i < deadBushes; i++) {
                placeOn(
                        region,
                        r,
                        cx,
                        cz,
                        Material.SAND,
                        Material.DEAD_BUSH
                );
            }
        }

        if (this.config.isFeatureEnabled("cactus")) {
            int cactus = featureCount(r, density * 0.28D);

            for (int i = 0; i < cactus; i++) {
                int x = (cx << 4) + 4 + r.nextInt(8);
                int z = (cz << 4) + 4 + r.nextInt(8);

                int y = findTopBlockY(region, x, z);

                if (y < 0) {
                    continue;
                }

                if (region.getType(x, y, z) != Material.SAND) {
                    continue;
                }

                placeCactus(region, r, x, y + 1, z);
            }
        }

        if (this.config.isFeatureEnabled("rocks")) {
            int rocks = featureCount(r, density * 0.35D);

            for (int i = 0; i < rocks; i++) {
                int x = (cx << 4) + 4 + r.nextInt(8);
                int z = (cz << 4) + 4 + r.nextInt(8);

                int y = findTopBlockY(region, x, z);

                if (y < 0) {
                    continue;
                }

                if (region.getType(x, y, z) != Material.SAND) {
                    continue;
                }

                placeSmallDesertRock(region, r, x, y + 1, z);
            }
        }
    }

    private void populateBadlands(
            Random r,
            int cx,
            int cz,
            LimitedRegion region,
            double density
    ) {
        if (this.config.isFeatureEnabled("dead-bushes")) {
            int deadBushes = featureCount(r, density * 1.1D);

            for (int i = 0; i < deadBushes; i++) {
                int x = (cx << 4) + r.nextInt(16);
                int z = (cz << 4) + r.nextInt(16);

                int y = findTopBlockY(region, x, z);

                if (y < 0) {
                    continue;
                }

                Material top = region.getType(x, y, z);

                if ((top == Material.RED_SAND || top == Material.TERRACOTTA)
                        && hasAir(region, x, y + 1, z)) {
                    region.setType(x, y + 1, z, Material.DEAD_BUSH);
                }
            }
        }

        if (this.config.isFeatureEnabled("rocks")) {
            int formations = featureCount(r, density * 0.45D);

            for (int i = 0; i < formations; i++) {
                int x = (cx << 4) + 4 + r.nextInt(8);
                int z = (cz << 4) + 4 + r.nextInt(8);

                int y = findTopBlockY(region, x, z);

                if (y < 0) {
                    continue;
                }

                Material top = region.getType(x, y, z);

                if (top != Material.RED_SAND
                        && top != Material.TERRACOTTA) {
                    continue;
                }

                placeBadlandsFormation(region, r, x, y + 1, z);
            }
        }
    }

    private void populateSnow(
            Random r,
            int cx,
            int cz,
            LimitedRegion region,
            double density
    ) {
        if (!this.config.isFeatureEnabled("small-trees")) {
            return;
        }

        int trees = featureCount(
                r,
                density
                        * this.config.getBiomeTreeDensity(
                                this.biomeType.getId()
                        )
                        * 0.15D
        );

        for (int i = 0; i < trees; i++) {
            int x = (cx << 4) + 5 + r.nextInt(6);
            int z = (cz << 4) + 5 + r.nextInt(6);

            int snowY = findTopSnowLayerY(region, x, z);

            if (snowY < 0) {
                continue;
            }

            int groundY = snowY - 1;
            int baseY = snowY;

            Material ground = region.getType(x, groundY, z);

            if (ground != Material.GRASS_BLOCK
                    && ground != Material.DIRT
                    && ground != Material.SNOW_BLOCK) {
                continue;
            }

            if (!canPlaceSpruce(region, x, baseY, z)) {
                continue;
            }

            placeSnowSpruce(region, x, baseY, z);
        }
    }

    private void populateMushroom(
            Random r,
            int cx,
            int cz,
            LimitedRegion region,
            double density
    ) {
        if (!this.config.isFeatureEnabled("mushrooms")) {
            return;
        }

        int smallMushrooms = featureCount(r, density * 2.8D);

        for (int i = 0; i < smallMushrooms; i++) {
            int x = (cx << 4) + r.nextInt(16);
            int z = (cz << 4) + r.nextInt(16);

            int y = findTopBlockY(region, x, z);

            if (y < 0) {
                continue;
            }

            if (region.getType(x, y, z) != Material.MYCELIUM) {
                continue;
            }

            if (!hasAir(region, x, y + 1, z)) {
                continue;
            }

            region.setType(
                    x,
                    y + 1,
                    z,
                    r.nextBoolean()
                            ? Material.RED_MUSHROOM
                            : Material.BROWN_MUSHROOM
            );
        }

        int largeMushrooms = featureCount(r, density * 0.45D);

        for (int i = 0; i < largeMushrooms; i++) {
            int x = (cx << 4) + 5 + r.nextInt(6);
            int z = (cz << 4) + 5 + r.nextInt(6);

            int y = findTopBlockY(region, x, z);

            if (y < 0) {
                continue;
            }

            if (region.getType(x, y, z) != Material.MYCELIUM) {
                continue;
            }

            int baseY = y + 1;

            if (!canPlaceMushroomStructure(region, x, baseY, z)) {
                continue;
            }

            placeLargeMushroom(region, r, x, baseY, z);
        }
    }

    private void populateEnd(
            Random r,
            int cx,
            int cz,
            LimitedRegion region,
            double density
    ) {
        if (!this.config.isFeatureEnabled("rocks")) {
            return;
        }

        int bumps = featureCount(r, density * 0.18D);

        for (int i = 0; i < bumps; i++) {
            int x = (cx << 4) + 5 + r.nextInt(6);
            int z = (cz << 4) + 5 + r.nextInt(6);

            int y = findTopBlockY(region, x, z);

            if (y < 0) {
                continue;
            }

            if (region.getType(x, y, z) != Material.END_STONE) {
                continue;
            }

            placeEndBump(region, r, x, y + 1, z);
        }
    }

    private void placeSnowSpruce(
            LimitedRegion r,
            int x,
            int yBase,
            int z
    ) {
        int trunkHeight = 7;

        for (int i = 0; i < trunkHeight; i++) {
            r.setType(x, yBase + i, z, Material.SPRUCE_LOG);
        }

        int topY = yBase + trunkHeight - 1;

        placeLeafLayer(r, x, topY - 4, z, 3);
        placeLeafLayer(r, x, topY - 3, z, 2);
        placeLeafLayer(r, x, topY - 2, z, 2);
        placeLeafLayer(r, x, topY - 1, z, 1);
        placeLeafLayer(r, x, topY, z, 1);

        placeLeafIfAir(
                r,
                x,
                topY + 1,
                z,
                Material.SPRUCE_LEAVES
        );

        placeSnowOnLeaves(r, x, topY - 2, z, 2);
        placeSnowOnLeaves(r, x, topY - 1, z, 1);
        placeSnowOnLeaves(r, x, topY, z, 1);
    }

    private void placeLargeMushroom(
            LimitedRegion r,
            Random rng,
            int x,
            int yBase,
            int z
    ) {
        boolean red = rng.nextBoolean();

        Material cap = red
                ? Material.RED_MUSHROOM_BLOCK
                : Material.BROWN_MUSHROOM_BLOCK;

        int stemHeight = 4 + rng.nextInt(3);

        for (int i = 0; i < stemHeight; i++) {
            r.setType(x, yBase + i, z, Material.MUSHROOM_STEM);
        }

        int capY = yBase + stemHeight;

        int lowerRadius = red ? 2 : 3;
        int upperRadius = red ? 1 : 2;

        placeMushroomCapLayer(r, x, capY, z, lowerRadius, cap);
        placeMushroomCapLayer(r, x, capY + 1, z, upperRadius, cap);

        if (red) {
            placeIfAir(r, x, capY + 2, z, cap);
        }
    }

    private void placeMushroomCapLayer(
            LimitedRegion r,
            int x,
            int y,
            int z,
            int radius,
            Material cap
    ) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                boolean corner =
                        Math.abs(dx) == radius
                                && Math.abs(dz) == radius;

                if (corner && radius > 1) {
                    continue;
                }

                placeIfAir(
                        r,
                        x + dx,
                        y,
                        z + dz,
                        cap
                );
            }
        }
    }

    private void placeLeafLayer(
            LimitedRegion r,
            int x,
            int y,
            int z,
            int radius
    ) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                if (Math.abs(dx) == radius
                        && Math.abs(dz) == radius) {
                    continue;
                }

                placeLeafIfAir(
                        r,
                        x + dx,
                        y,
                        z + dz,
                        Material.SPRUCE_LEAVES
                );
            }
        }
    }

    private void placeSnowOnLeaves(
            LimitedRegion r,
            int x,
            int y,
            int z,
            int radius
    ) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                int px = x + dx;
                int py = y + 1;
                int pz = z + dz;

                Material below = r.getType(px, y, pz);
                Material target = r.getType(px, py, pz);

                if (below == Material.SPRUCE_LEAVES
                        && target == Material.AIR) {
                    r.setType(px, py, pz, Material.SNOW);
                }
            }
        }
    }

    private void placeSmallOakBush(
            LimitedRegion r,
            int x,
            int yBase,
            int z
    ) {
        if (!hasAir(r, x, yBase, z)) {
            return;
        }

        r.setType(x, yBase, z, Material.OAK_LEAVES);

        placeLeafIfAir(r, x + 1, yBase, z, Material.OAK_LEAVES);
        placeLeafIfAir(r, x - 1, yBase, z, Material.OAK_LEAVES);
        placeLeafIfAir(r, x, yBase, z + 1, Material.OAK_LEAVES);
        placeLeafIfAir(r, x, yBase, z - 1, Material.OAK_LEAVES);
    }

    private void placeCactus(
            LimitedRegion r,
            Random rng,
            int x,
            int yBase,
            int z
    ) {
        int height = 1 + rng.nextInt(3);

        for (int i = 0; i < height; i++) {
            int y = yBase + i;

            if (!hasAir(r, x, y, z)) {
                return;
            }

            if (!hasAir(r, x - 1, y, z)
                    || !hasAir(r, x + 1, y, z)
                    || !hasAir(r, x, y, z - 1)
                    || !hasAir(r, x, y, z + 1)) {
                return;
            }
        }

        for (int i = 0; i < height; i++) {
            r.setType(x, yBase + i, z, Material.CACTUS);
        }
    }

    private void placeSmallDesertRock(
            LimitedRegion r,
            Random rng,
            int x,
            int yBase,
            int z
    ) {
        if (!hasSolidBelow(r, x, yBase, z)) {
            return;
        }

        if (hasAir(r, x, yBase, z)) {
            r.setType(x, yBase, z, Material.SANDSTONE);
        }

        if (rng.nextBoolean() && hasAir(r, x, yBase + 1, z)) {
            r.setType(x, yBase + 1, z, Material.SANDSTONE);
        }
    }

    private void placeBadlandsFormation(
            LimitedRegion r,
            Random rng,
            int x,
            int yBase,
            int z
    ) {
        if (!hasSolidBelow(r, x, yBase, z)) {
            return;
        }

        int height = 1 + rng.nextInt(2);
        int radius = 1;

        for (int dy = 0; dy < height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    if (Math.abs(dx) + Math.abs(dz) > radius + 1) {
                        continue;
                    }

                    int px = x + dx;
                    int py = yBase + dy;
                    int pz = z + dz;

                    if (hasAir(r, px, py, pz)
                            && hasSolidBelow(r, px, py, pz)) {

                        r.setType(
                                px,
                                py,
                                pz,
                                BADLANDS_COLORS[
                                        rng.nextInt(BADLANDS_COLORS.length)
                                ]
                        );
                    }
                }
            }

            radius = Math.max(0, radius - 1);
        }
    }

    private void placeEndBump(
            LimitedRegion r,
            Random rng,
            int x,
            int yBase,
            int z
    ) {
        if (!hasSolidBelow(r, x, yBase, z)) {
            return;
        }

        if (hasAir(r, x, yBase, z)) {
            r.setType(x, yBase, z, Material.END_STONE);
        }

        if (rng.nextInt(4) == 0 && hasAir(r, x, yBase + 1, z)) {
            r.setType(x, yBase + 1, z, Material.END_STONE);
        }
    }

    private boolean canPlaceSpruce(
            LimitedRegion r,
            int x,
            int yBase,
            int z
    ) {
        for (int y = yBase; y <= yBase + 10; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {

                    Material m = r.getType(x + dx, y, z + dz);

                    if (m != Material.AIR
                            && m != Material.SNOW) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean canPlaceMushroomStructure(
            LimitedRegion r,
            int x,
            int yBase,
            int z
    ) {
        if (!hasSolidBelow(r, x, yBase, z)) {
            return false;
        }

        for (int y = yBase; y <= yBase + 8; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {

                    Material m = r.getType(x + dx, y, z + dz);

                    if (m != Material.AIR) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void placeOnGrass(
            LimitedRegion r,
            Random rng,
            int cx,
            int cz,
            Material what
    ) {
        placeOn(r, rng, cx, cz, Material.GRASS_BLOCK, what);
    }

    private void placeOn(
            LimitedRegion r,
            Random rng,
            int cx,
            int cz,
            Material requiredBase,
            Material what
    ) {
        int x = (cx << 4) + rng.nextInt(16);
        int z = (cz << 4) + rng.nextInt(16);

        int y = findTopBlockY(r, x, z);

        if (y < 0) {
            return;
        }

        if (r.getType(x, y, z) != requiredBase) {
            return;
        }

        if (!hasAir(r, x, y + 1, z)) {
            return;
        }

        r.setType(x, y + 1, z, what);
    }

    private void placeLeafIfAir(
            LimitedRegion r,
            int x,
            int y,
            int z,
            Material leaf
    ) {
        Material current = r.getType(x, y, z);

        if (current == Material.AIR
                || current == Material.SNOW) {
            r.setType(x, y, z, leaf);
        }
    }

    private void placeIfAir(
            LimitedRegion r,
            int x,
            int y,
            int z,
            Material material
    ) {
        if (hasAir(r, x, y, z)) {
            r.setType(x, y, z, material);
        }
    }

    private int findTopBlockY(
            LimitedRegion r,
            int x,
            int z
    ) {
        int baseHeight = this.config.getBaseHeight();
        int variation =
                (int) Math.ceil(this.config.getHeightVariation());

        int startY = baseHeight + variation + 12;
        int endY = baseHeight - variation - 8;

        for (int y = startY; y >= endY; y--) {
            Material m = r.getType(x, y, z);

            if (isValidTerrainSurface(m)) {
                return y;
            }
        }

        return -1;
    }

    private int findTopSnowLayerY(
            LimitedRegion r,
            int x,
            int z
    ) {
        int baseHeight = this.config.getBaseHeight();
        int variation =
                (int) Math.ceil(this.config.getHeightVariation());

        int startY = baseHeight + variation + 12;
        int endY = baseHeight - variation - 8;

        for (int y = startY; y >= endY; y--) {
            if (r.getType(x, y, z) == Material.SNOW) {
                return y;
            }
        }

        return -1;
    }

    private boolean isValidTerrainSurface(Material m) {
        return switch (m) {

            case GRASS_BLOCK,
                 DIRT,
                 PODZOL,
                 SAND,
                 RED_SAND,
                 SANDSTONE,
                 TERRACOTTA,
                 ORANGE_TERRACOTTA,
                 YELLOW_TERRACOTTA,
                 RED_TERRACOTTA,
                 BROWN_TERRACOTTA,
                 WHITE_TERRACOTTA,
                 MYCELIUM,
                 SNOW,
                 SNOW_BLOCK,
                 STONE,
                 DEEPSLATE,
                 END_STONE -> true;

            default -> false;
        };
    }

    private boolean hasAir(
            LimitedRegion r,
            int x,
            int y,
            int z
    ) {
        return r.getType(x, y, z) == Material.AIR;
    }

    private boolean hasSolidBelow(
            LimitedRegion r,
            int x,
            int y,
            int z
    ) {
        return isSolidBase(r.getType(x, y - 1, z));
    }

    private boolean isSolidBase(Material m) {
        return switch (m) {

            case GRASS_BLOCK,
                 DIRT,
                 PODZOL,
                 SAND,
                 RED_SAND,
                 SANDSTONE,
                 TERRACOTTA,
                 MYCELIUM,
                 SNOW_BLOCK,
                 STONE,
                 DEEPSLATE,
                 END_STONE -> true;

            default -> false;
        };
    }

    private int featureCount(
            Random r,
            double expected
    ) {
        if (expected <= 0.0D) {
            return 0;
        }

        int whole = (int) Math.floor(expected);
        double frac = expected - whole;

        if (r.nextDouble() < frac) {
            whole++;
        }

        return whole;
    }
}
