package buildcraft.factory.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TubeBlock extends Block {
    public static final MapCodec<TubeBlock> CODEC = simpleCodec(TubeBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public TubeBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(-1.0F, 3_600_000.0F).sound(SoundType.METAL).noOcclusion().noLootTable());
    }

    @Override protected MapCodec<? extends TubeBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
