package me.liass.cpvpsinglebiome.populator;

import java.util.Random;

import me.liass.cpvpsinglebiome.config.ConfigManager;
import me.liass.cpvpsinglebiome.generator.BiomeType;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
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

    /*
     * Snow rocks are intentionally not too dark.
     * They should work as small orientation details, not as dirty stone piles.
     */
    private static final Material[] SNOW_ROCK_MATERIALS = new Material[] {
            Material.STONE,
            Material.ANDESITE,
            Material.CALCITE,
            Material.PACKED_ICE
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
        populateSnowLayers(r, cx, cz, region, density);
        populateSnowIceDetails(r, cx, cz, region);
        populateSnowRocks(r, cx, cz, region);
        populateSnowTrees(r, cx, cz, region, density);
    }

    private void populateSnowLayers(
            Random r,
            int cx,
            int cz,
            LimitedRegion region,
            double density
    ) {
        if (!this.config.isSnowLayersEnabled()) {
            return;
        }

        double chance =
                clampProbability(
                        this.config.getSnowLayerChance()
                );

        int attempts =
                Math.max(
                        4,
                        featureCount(r, 16.0D * density)
                );

        for (int i = 0; i < attempts; i++) {
            if (r.nextDouble() > chance) {
                continue;
            }

            int x = (cx << 4) + r.nextInt(16);
            int z = (cz << 4) + r.nextInt(16);

            int groundY =
                    findSnowGroundY(region, x, z);

            if (groundY < 0) {
                continue;
            }

            Material ground =
                    region.getType(x, groundY, z);

            if (!canPlaceSnowLayerOn(ground)) {
                continue;
            }

            int targetY =
                    groundY + 1;

            if (!hasAir(region, x, targetY, z)) {
                continue;
            }

            placeSnowLayer(
                    region,
                    r,
                    x,
                    targetY,
                    z
            );
        }
    }

    private void populateSnowIceDetails(
            Random r,
            int cx,
            int cz,
            LimitedRegion region
    ) {
        if (!this.config.isSnowIceDetailsEnabled()) {
            return;
        }

        /*
         * Bigger frozen ponds:
         * Radius is intentionally weighted toward the upper range.
         * With max-pond-radius: 5 this produces mostly radius 3-5,
         * not lots of tiny radius 1-2 ponds.
         */
        if (r.nextDouble()
                < clampProbability(
                        this.config.getSnowPondChance()
                )) {
            int x = (cx << 4) + 5 + r.nextInt(6);
            int z = (cz << 4) + 5 + r.nextInt(6);

            int groundY =
                    findSnowGroundY(region, x, z);

            if (groundY >= 0) {
                int radius =
                        chooseSnowPondRadius(r);

                placeFrozenPond(
                        region,
                        r,
                        x,
                        groundY,
                        z,
                        radius
                );
            }
        }

        /*
         * Small patches remain small by design.
         * They are only minor accents, while ponds provide the larger ice areas.
         */
        if (r.nextDouble()
                < clampProbability(
                        this.config.getSnowSmallIcePatchChance()
                )) {
            int x = (cx << 4) + 4 + r.nextInt(8);
            int z = (cz << 4) + 4 + r.nextInt(8);

            int groundY =
                    findSnowGroundY(region, x, z);

            if (groundY >= 0) {
                int radius =
                        chooseSmallIcePatchRadius(r);

                placeSmallIcePatch(
                        region,
                        r,
                        x,
                        groundY,
                        z,
                        radius
                );
            }
        }
    }

    private int chooseSnowPondRadius(Random r) {
        int maxRadius =
                Math.max(
                        1,
                        this.config.getSnowMaxPondRadius()
                );

        maxRadius =
                Math.min(
                        maxRadius,
                        5
                );

        if (maxRadius <= 2) {
            return maxRadius;
        }

        int minRadius =
                Math.max(
                        3,
                        maxRadius - 2
                );

        return minRadius
                + r.nextInt(
                        maxRadius - minRadius + 1
                );
    }

    private int chooseSmallIcePatchRadius(Random r) {
        int maxRadius =
                Math.max(
                        1,
                        this.config.getSnowMaxIcePatchRadius()
                );

        maxRadius =
                Math.min(
                        maxRadius,
                        3
                );

        if (maxRadius <= 1) {
            return 1;
        }

        return 1 + r.nextInt(maxRadius);
    }

    private void populateSnowRocks(
            Random r,
            int cx,
            int cz,
            LimitedRegion region
    ) {
        if (!this.config.isSnowRocksEnabled()) {
            return;
        }

        if (r.nextDouble()
                > clampProbability(
                        this.config.getSnowRockChance()
                )) {
            return;
        }

        int x = (cx << 4) + 4 + r.nextInt(8);
        int z = (cz << 4) + 4 + r.nextInt(8);

        int groundY =
                findSnowGroundY(region, x, z);

        if (groundY < 0) {
            return;
        }

        Material ground =
                region.getType(x, groundY, z);

        if (!canDecorateSnowGround(ground)) {
            return;
        }

        placeSmallSnowRock(
                region,
                r,
                x,
                groundY + 1,
                z,
                Math.max(
                        1,
                        this.config.getSnowRockMaxSize()
                )
        );
    }

    private void populateSnowTrees(
            Random r,
            int cx,
            int cz,
            LimitedRegion region,
            double density
    ) {
        if (!this.config.isSnowTreesEnabled()) {
            return;
        }

        if (!this.config.isFeatureEnabled("small-trees")) {
            return;
        }

        /*
         * snow-effects.trees.chance is intentionally used directly
         * as chance per chunk.
         *
         * Do not multiply it with decoration density again.
         * Otherwise values like 0.04 become almost invisible in practice.
         */
        double chance =
                clampProbability(
                        this.config.getSnowTreeChance()
                );

        if (r.nextDouble() > chance) {
            return;
        }

        int x = (cx << 4) + 5 + r.nextInt(6);
        int z = (cz << 4) + 5 + r.nextInt(6);

        int groundY =
                findSnowGroundY(region, x, z);

        if (groundY < 0) {
            return;
        }

        Material ground =
                region.getType(x, groundY, z);

        if (ground != Material.GRASS_BLOCK
                && ground != Material.DIRT
                && ground != Material.SNOW_BLOCK) {
            return;
        }

        int baseY =
                groundY + 1;

        int trunkHeight =
                chooseSnowSpruceTrunkHeight(r);

        if (!canPlaceSpruce(region, x, baseY, z, trunkHeight)) {
            return;
        }

        placeSnowSpruce(region, x, baseY, z, trunkHeight);
    }

    private int chooseSnowSpruceTrunkHeight(Random r) {
        int roll =
                r.nextInt(100);

        if (roll < 50) {
            return 4;
        }

        if (roll < 82) {
            return 5;
        }

        if (roll < 96) {
            return 6;
        }

        return 7;
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

    private void placeFrozenPond(
            LimitedRegion region,
            Random r,
            int centerX,
            int centerY,
            int centerZ,
            int radius
    ) {
        /*
         * Frozen ponds are placed flat on centerY.
         *
         * Reason:
         * A frozen pond should look like a frozen surface, not like stairs.
         * We only accept nearby terrain with a height difference of 1 block.
         * Steeper terrain is skipped so the pond naturally breaks at slopes.
         */
        int safeRadius =
                Math.max(1, Math.min(radius, 5));

        int iceY =
                centerY;

        for (int dx = -safeRadius; dx <= safeRadius; dx++) {
            for (int dz = -safeRadius; dz <= safeRadius; dz++) {

                double distance =
                        Math.sqrt(
                                (dx * dx)
                                        + (dz * dz)
                        );

                if (distance > safeRadius + r.nextDouble() * 0.35D) {
                    continue;
                }

                int x = centerX + dx;
                int z = centerZ + dz;

                tryPlaceFlatIceCell(
                        region,
                        x,
                        iceY,
                        z
                );
            }
        }
    }

    private void placeSmallIcePatch(
            LimitedRegion region,
            Random r,
            int centerX,
            int centerY,
            int centerZ,
            int radius
    ) {
        /*
         * Small ice patches are also kept flat.
         * They are capped lower than ponds and should stay as minor accents.
         */
        int safeRadius =
                Math.max(1, Math.min(radius, 3));

        int iceY =
                centerY;

        for (int dx = -safeRadius; dx <= safeRadius; dx++) {
            for (int dz = -safeRadius; dz <= safeRadius; dz++) {

                if (Math.abs(dx) + Math.abs(dz)
                        > safeRadius + r.nextInt(2)) {
                    continue;
                }

                int x = centerX + dx;
                int z = centerZ + dz;

                tryPlaceFlatIceCell(
                        region,
                        x,
                        iceY,
                        z
                );
            }
        }
    }

    private boolean tryPlaceFlatIceCell(
            LimitedRegion region,
            int x,
            int iceY,
            int z
    ) {
        int groundY =
                findSnowGroundY(region, x, z);

        if (groundY < 0) {
            return false;
        }

        /*
         * Allow only gentle terrain around the pond.
         * This prevents ugly stepped ice while avoiding floating plates.
         */
        if (Math.abs(groundY - iceY) > 1) {
            return false;
        }

        Material ground =
                region.getType(x, groundY, z);

        if (!canDecorateSnowGround(ground)) {
            return false;
        }

        ensureIceSupport(
                region,
                x,
                iceY,
                z
        );

        clearIceSpaceAbove(
                region,
                x,
                iceY,
                z
        );

        region.setType(
                x,
                iceY,
                z,
                chooseSnowIceMaterial()
        );

        return true;
    }

    private void ensureIceSupport(
            LimitedRegion region,
            int x,
            int iceY,
            int z
    ) {
        Material below =
                region.getType(x, iceY - 1, z);

        if (isSolidBase(below)) {
            return;
        }

        /*
         * If the local terrain is one block lower, support the flat ice.
         * Dirt is hidden below the ice and avoids floating pond edges.
         */
        if (below == Material.AIR
                || below == Material.SNOW) {
            region.setType(
                    x,
                    iceY - 1,
                    z,
                    Material.DIRT
            );
        }
    }

    private void clearIceSpaceAbove(
            LimitedRegion region,
            int x,
            int iceY,
            int z
    ) {
        /*
         * Clear snow and minor terrain that would otherwise sit on top of
         * a flat pond. Only two blocks are touched to avoid ugly cuts.
         */
        for (int y = iceY + 1; y <= iceY + 2; y++) {
            Material current =
                    region.getType(x, y, z);

            if (current == Material.AIR) {
                continue;
            }

            if (isIceSpaceClearable(current)) {
                region.setType(
                        x,
                        y,
                        z,
                        Material.AIR
                );
            }
        }
    }

    private boolean isIceSpaceClearable(Material material) {
        return switch (material) {

            case SNOW,
                 SHORT_GRASS,
                 TALL_GRASS,
                 FERN,
                 GRASS_BLOCK,
                 DIRT,
                 PODZOL,
                 SNOW_BLOCK,
                 STONE,
                 ANDESITE,
                 CALCITE -> true;

            default -> false;
        };
    }

    private Material chooseSnowIceMaterial() {
        /*
         * Use only PACKED_ICE for generated ice areas.
         *
         * Reason:
         * - real ice look
         * - slight slippery movement
         * - no mixed blue tones
         * - no dark BLUE_ICE spots
         */
        return Material.PACKED_ICE;
    }

    private void placeSmallSnowRock(
            LimitedRegion region,
            Random r,
            int centerX,
            int yBase,
            int centerZ,
            int maxSize
    ) {
        if (!hasSolidBelow(region, centerX, yBase, centerZ)) {
            return;
        }

        int blocks =
                1 + r.nextInt(
                        Math.max(
                                1,
                                Math.min(maxSize, 5)
                        )
                );

        for (int i = 0; i < blocks; i++) {
            int dx = r.nextInt(3) - 1;
            int dz = r.nextInt(3) - 1;
            int dy = r.nextInt(3) == 0 ? 1 : 0;

            int x = centerX + dx;
            int y = yBase + dy;
            int z = centerZ + dz;

            if (!isReplaceableSnowDetail(region.getType(x, y, z))) {
                continue;
            }

            if (!hasSolidBelow(region, x, y, z)) {
                continue;
            }

            Material material =
                    SNOW_ROCK_MATERIALS[
                            r.nextInt(SNOW_ROCK_MATERIALS.length)
                    ];

            region.setType(
                    x,
                    y,
                    z,
                    material
            );
        }
    }

    private void placeSnowLayer(
            LimitedRegion region,
            Random r,
            int x,
            int y,
            int z
    ) {
        int maxLayers =
                Math.max(
                        1,
                        Math.min(
                                8,
                                this.config.getSnowMaxLayerHeight()
                        )
                );

        int layers =
                1 + r.nextInt(maxLayers);

        BlockData data =
                Material.SNOW.createBlockData();

        if (data instanceof Snow snow) {
            snow.setLayers(layers);
            region.setBlockData(x, y, z, snow);
            return;
        }

        region.setType(x, y, z, Material.SNOW);
    }

    private void placeSnowSpruce(
            LimitedRegion r,
            int x,
            int yBase,
            int z,
            int trunkHeight
    ) {
        int safeTrunkHeight =
                Math.max(
                        4,
                        Math.min(
                                trunkHeight,
                                7
                        )
                );

        for (int i = 0; i < safeTrunkHeight; i++) {
            r.setType(x, yBase + i, z, Material.SPRUCE_LOG);
        }

        int topY =
                yBase + safeTrunkHeight - 1;

        if (safeTrunkHeight >= 6) {
            placeLeafLayer(r, x, topY - 4, z, 2);
        }

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

        if (safeTrunkHeight >= 6) {
            placeSnowOnLeaves(r, x, topY - 2, z, 2);
        }

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
            int z,
            int trunkHeight
    ) {
        int safeTrunkHeight =
                Math.max(
                        4,
                        Math.min(
                                trunkHeight,
                                7
                        )
                );

        int maxY =
                yBase + safeTrunkHeight + 2;

        for (int y = yBase; y <= maxY; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {

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

    private int findSnowGroundY(
            LimitedRegion r,
            int x,
            int z
    ) {
        int y =
                findTopBlockY(r, x, z);

        if (y < 0) {
            return -1;
        }

        Material top =
                r.getType(x, y, z);

        if (top == Material.SNOW) {
            return y - 1;
        }

        return y;
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
                 ANDESITE,
                 CALCITE,
                 DEEPSLATE,
                 END_STONE,
                 ICE,
                 PACKED_ICE,
                 BLUE_ICE -> true;

            default -> false;
        };
    }

    private boolean canPlaceSnowLayerOn(Material m) {
        return switch (m) {

            case GRASS_BLOCK,
                 DIRT,
                 PODZOL,
                 SNOW_BLOCK,
                 STONE,
                 ANDESITE,
                 CALCITE -> true;

            default -> false;
        };
    }

    private boolean canDecorateSnowGround(Material m) {
        return switch (m) {

            case GRASS_BLOCK,
                 DIRT,
                 PODZOL,
                 SNOW_BLOCK,
                 STONE,
                 ANDESITE,
                 CALCITE -> true;

            default -> false;
        };
    }

    private boolean isReplaceableSnowDetail(Material m) {
        return m == Material.AIR
                || m == Material.SNOW;
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
                 ANDESITE,
                 CALCITE,
                 DEEPSLATE,
                 END_STONE,
                 ICE,
                 PACKED_ICE,
                 BLUE_ICE -> true;

            default -> false;
        };
    }

    private double clampProbability(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }

        if (value > 1.0D) {
            return 1.0D;
        }

        return value;
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
