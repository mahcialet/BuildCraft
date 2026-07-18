package buildcraft.core.gen;

import buildcraft.api.enums.EnumSpring;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.block.BlockSpring;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Rare overworld spring placed into the lowest bedrock layers. */
public final class WaterSpringFeature extends Feature<NoneFeatureConfiguration> {
    public WaterSpringFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!EnumSpring.WATER.canGen) return false;
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minY = level.getMinY();
        for (int y = minY; y < minY + 5; y++) {
            BlockPos candidate = new BlockPos(origin.getX(), y, origin.getZ());
            if (!level.getBlockState(candidate).is(Blocks.BEDROCK)) continue;

            BlockState spring = BCCoreBlocks.SPRING.get().defaultBlockState()
                .setValue(BlockSpring.SPRING_TYPE, EnumSpring.WATER);
            level.setBlock(candidate, spring, Block.UPDATE_ALL);
            for (int fillY = y + 2; fillY < level.getMaxY(); fillY++) {
                BlockPos fill = new BlockPos(origin.getX(), fillY, origin.getZ());
                if (level.isEmptyBlock(fill)) break;
                level.setBlock(fill, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            }
            return true;
        }
        return false;
    }

}
