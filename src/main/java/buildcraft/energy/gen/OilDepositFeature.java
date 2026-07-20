package buildcraft.energy.gen;

import buildcraft.api.enums.EnumSpring;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.block.BlockSpring;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.BCEnergyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/** Deterministic, cross-chunk reconstruction of BuildCraft's classic oil lakes, wells, and spouts. */
public final class OilDepositFeature extends Feature<NoneFeatureConfiguration> {
    private static final long MAGIC_GEN_NUMBER = 0xD046B4E40C7D07CFL;
    private static final int MAX_CHUNK_RADIUS = 5;
    private enum DepositType { LARGE, MEDIUM, SMALL }

    public OilDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!BCEnergyConfig.ENABLE_OIL_GENERATION.get()) return false;
        WorldGenLevel level = context.level();
        int targetChunkX = context.origin().getX() >> 4;
        int targetChunkZ = context.origin().getZ() >> 4;
        boolean placed = false;
        for (int dx = -MAX_CHUNK_RADIUS; dx <= MAX_CHUNK_RADIUS; dx++) {
            for (int dz = -MAX_CHUNK_RADIUS; dz <= MAX_CHUNK_RADIUS; dz++) {
                int sourceChunkX = targetChunkX + dx;
                int sourceChunkZ = targetChunkZ + dz;
                Random random = randomForChunk(level.getSeed(), sourceChunkX, sourceChunkZ);
                int x = sourceChunkX * 16 + 8 + random.nextInt(16);
                int z = sourceChunkZ * 16 + 8 + random.nextInt(16);
                DepositType type;
                double rate = BCEnergyConfig.OIL_GENERATION_RATE.get();
                if (random.nextDouble() <= BCEnergyConfig.LARGE_OIL_CHANCE.get() / 100.0 * rate) {
                    type = DepositType.LARGE;
                } else if (random.nextDouble() <= BCEnergyConfig.MEDIUM_OIL_CHANCE.get() / 100.0 * rate) {
                    type = DepositType.MEDIUM;
                } else if ((level.getBiome(new BlockPos(x, 0, z)).is(OilBiomeReplacement.OIL_OCEAN)
                        || level.getBiome(new BlockPos(x, 0, z)).is(OilBiomeReplacement.OIL_DESERT))
                    && random.nextDouble() <= BCEnergyConfig.SMALL_OIL_CHANCE.get() / 100.0 * rate) {
                    type = DepositType.SMALL;
                } else continue;
                placed |= placeDeposit(level, targetChunkX, targetChunkZ, new BlockPos(x, 0, z), random, type);
            }
        }
        return placed;
    }

    private static Random randomForChunk(long worldSeed, int chunkX, int chunkZ) {
        Random worldRandom = new Random(worldSeed);
        long xSeed = worldRandom.nextLong() >> 3;
        long zSeed = worldRandom.nextLong() >> 3;
        return new Random(((xSeed * chunkX + zSeed * chunkZ) ^ worldSeed) ^ MAGIC_GEN_NUMBER);
    }

    private static boolean placeDeposit(WorldGenLevel level, int targetChunkX, int targetChunkZ,
                                        BlockPos horizontalCenter, Random random, DepositType type) {
        int lakeRadius = type == DepositType.LARGE ? 4 : type == DepositType.MEDIUM ? 2 : 1;
        int tendrilRadius = type == DepositType.LARGE ? 25 + random.nextInt(20)
            : type == DepositType.MEDIUM ? 5 + random.nextInt(10) : 2 + random.nextInt(4);
        boolean[][] pattern = tendrilPattern(lakeRadius, tendrilRadius, random);
        int depth = random.nextDouble() < 0.5 ? 1 : 2;
        boolean placed = placeSurfacePattern(level, targetChunkX, targetChunkZ,
                horizontalCenter, tendrilRadius, pattern, depth);

        int wellY = 20 + random.nextInt(10);
        int reservoirRadius = type == DepositType.LARGE ? 8 + random.nextInt(9)
            : type == DepositType.MEDIUM ? 4 + random.nextInt(4) : 2 + random.nextInt(2);
        BlockPos reservoir = new BlockPos(horizontalCenter.getX(), wellY, horizontalCenter.getZ());
        placed |= placeSphere(level, targetChunkX, targetChunkZ, reservoir, reservoirRadius);

        int spoutRadius = type == DepositType.LARGE ? 1 : 0;
        if (BCEnergyConfig.ENABLE_OIL_SPOUTS.get()) {
            int minSpout = type == DepositType.LARGE ? BCEnergyConfig.LARGE_SPOUT_MIN_HEIGHT.get()
                : BCEnergyConfig.SMALL_SPOUT_MIN_HEIGHT.get();
            int maxSpout = type == DepositType.LARGE ? BCEnergyConfig.LARGE_SPOUT_MAX_HEIGHT.get()
                : BCEnergyConfig.SMALL_SPOUT_MAX_HEIGHT.get();
            if (maxSpout < minSpout) maxSpout = minSpout;
            int spoutHeight = minSpout + random.nextInt(maxSpout - minSpout + 1);
            placed |= placeSpout(level, targetChunkX, targetChunkZ, reservoir, spoutRadius, spoutHeight);
        }

        if (type == DepositType.LARGE) {
            int bottom = level.getMinY();
            placed |= placeVerticalTube(level, targetChunkX, targetChunkZ,
                    horizontalCenter.getX(), horizontalCenter.getZ(), bottom + 1, wellY, spoutRadius);
            if ((horizontalCenter.getX() >> 4) == targetChunkX && (horizontalCenter.getZ() >> 4) == targetChunkZ) {
                level.setBlock(new BlockPos(horizontalCenter.getX(), bottom, horizontalCenter.getZ()),
                        BCCoreBlocks.SPRING.get().defaultBlockState()
                                .setValue(BlockSpring.SPRING_TYPE, EnumSpring.OIL), Block.UPDATE_ALL);
                placed = true;
            }
        }
        return placed;
    }

    public static boolean placeMediumForTest(WorldGenLevel level, BlockPos center, long seed) {
        return placeDeposit(level, center.getX() >> 4, center.getZ() >> 4, center,
            new Random(seed), DepositType.MEDIUM);
    }

    public static boolean placeSmallForTest(WorldGenLevel level, BlockPos center, long seed) {
        return placeDeposit(level, center.getX() >> 4, center.getZ() >> 4, center,
            new Random(seed), DepositType.SMALL);
    }

    public static boolean placeLargeForTest(WorldGenLevel level, BlockPos center, long seed) {
        return placeDeposit(level, center.getX() >> 4, center.getZ() >> 4, center,
                new Random(seed), DepositType.LARGE);
    }

    private static boolean[][] tendrilPattern(int lakeRadius, int radius, Random random) {
        int diameter = radius * 2 + 1;
        boolean[][] pattern = new boolean[diameter][diameter];
        for (int dx = -lakeRadius; dx <= lakeRadius; dx++) {
            for (int dz = -lakeRadius; dz <= lakeRadius; dz++) {
                pattern[radius + dx][radius + dz] = dx * dx + dz * dz <= lakeRadius * lakeRadius;
            }
        }
        for (int width = 1; width < radius; width++) {
            float chance = (radius - width + 4F) / (radius + 4F);
            connect(random, chance, radius, radius + width, pattern);
            connect(random, chance, radius, radius - width, pattern);
            connect(random, chance, radius + width, radius, pattern);
            connect(random, chance, radius - width, radius, pattern);
            for (int offset = 1; offset <= width; offset++) {
                connect(random, chance, radius + offset, radius + width, pattern);
                connect(random, chance, radius + offset, radius - width, pattern);
                connect(random, chance, radius + width, radius + offset, pattern);
                connect(random, chance, radius - width, radius + offset, pattern);
                connect(random, chance, radius - offset, radius + width, pattern);
                connect(random, chance, radius - offset, radius - width, pattern);
                connect(random, chance, radius + width, radius - offset, pattern);
                connect(random, chance, radius - width, radius - offset, pattern);
            }
        }
        return pattern;
    }

    private static void connect(Random random, float chance, int x, int z, boolean[][] pattern) {
        if (random.nextFloat() > chance || x < 0 || z < 0 || x >= pattern.length || z >= pattern.length) return;
        pattern[x][z] = isSet(pattern, x - 1, z) || isSet(pattern, x + 1, z)
                || isSet(pattern, x, z - 1) || isSet(pattern, x, z + 1);
    }

    private static boolean isSet(boolean[][] pattern, int x, int z) {
        return x >= 0 && z >= 0 && x < pattern.length && z < pattern.length && pattern[x][z];
    }

    private static boolean placeSurfacePattern(WorldGenLevel level, int chunkX, int chunkZ, BlockPos center,
                                               int radius, boolean[][] pattern, int depth) {
        boolean placed = false;
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        for (int x = minX; x < minX + 16; x++) {
            int px = x - (center.getX() - radius);
            if (px < 0 || px >= pattern.length) continue;
            for (int z = minZ; z < minZ + 16; z++) {
                int pz = z - (center.getZ() - radius);
                if (pz < 0 || pz >= pattern.length || !pattern[px][pz]) continue;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                for (int clear = 1; clear <= 5; clear++) {
                    level.removeBlock(new BlockPos(x, top + clear, z), false);
                }
                for (int dy = 0; dy < depth; dy++) setOil(level, new BlockPos(x, top - dy, z));
                placed = true;
            }
        }
        return placed;
    }

    private static boolean placeSphere(WorldGenLevel level, int chunkX, int chunkZ,
                                       BlockPos center, int radius) {
        boolean placed = false;
        int minX = Math.max(chunkX * 16, center.getX() - radius);
        int maxX = Math.min(chunkX * 16 + 15, center.getX() + radius);
        int minZ = Math.max(chunkZ * 16, center.getZ() - radius);
        int maxZ = Math.min(chunkZ * 16 + 15, center.getZ() + radius);
        int radiusSq = radius * radius;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                int dx = x - center.getX(), dy = y - center.getY(), dz = z - center.getZ();
                if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                    setOil(level, new BlockPos(x, y, z));
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean placeSpout(WorldGenLevel level, int chunkX, int chunkZ,
                                      BlockPos reservoir, int radius, int height) {
        if ((reservoir.getX() >> 4) != chunkX || (reservoir.getZ() >> 4) != chunkZ) return false;
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, reservoir.getX(), reservoir.getZ()) - 1;
        placeVerticalTube(level, chunkX, chunkZ, reservoir.getX(), reservoir.getZ(),
                reservoir.getY(), surface, radius);
        int y = surface;
        for (int currentRadius = radius; currentRadius >= 0; currentRadius--) {
            placeVerticalTube(level, chunkX, chunkZ, reservoir.getX(), reservoir.getZ(),
                    y, y + height, currentRadius);
            y += height;
        }
        return true;
    }

    private static boolean placeVerticalTube(WorldGenLevel level, int chunkX, int chunkZ,
                                             int centerX, int centerZ, int minY, int maxY, int radius) {
        boolean placed = false;
        int radiusSq = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            if ((x >> 4) != chunkX) continue;
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if ((z >> 4) != chunkZ) continue;
                int dx = x - centerX, dz = z - centerZ;
                if (dx * dx + dz * dz > radiusSq) continue;
                for (int y = minY; y <= maxY; y++) setOil(level, new BlockPos(x, y, z));
                placed = true;
            }
        }
        return placed;
    }

    private static void setOil(WorldGenLevel level, BlockPos pos) {
        BlockState oil = BCEnergyFluids.OIL_BLOCK.get().defaultBlockState();
        level.setBlock(pos, oil, Block.UPDATE_CLIENTS);
    }
}
