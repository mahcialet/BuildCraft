package buildcraft.factory.block;

import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.block.entity.DistillerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public final class DistillerBlock extends BaseEntityBlock {
    public static final MapCodec<DistillerBlock> CODEC = simpleCodec(DistillerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DistillerBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(2.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.WEST));
    }

    @Override protected MapCodec<? extends DistillerBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DistillerBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BCFactoryBlockEntities.DISTILLER.get(), DistillerBlockEntity::tick);
    }
}
