package buildcraft.core.block;

import buildcraft.core.marker.PathSavedData;
import com.mojang.serialization.MapCodec;
import buildcraft.core.block.entity.PathMarkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Thin, face-mounted marker whose ordered connections live in {@link PathSavedData}. */
public final class BlockMarkerPath extends BaseEntityBlock {
    public static final MapCodec<BlockMarkerPath> CODEC = simpleCodec(BlockMarkerPath::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final VoxelShape UP = box(6, 0, 6, 10, 10, 10);
    private static final VoxelShape DOWN = box(6, 6, 6, 10, 16, 10);
    private static final VoxelShape NORTH = box(6, 6, 6, 10, 10, 16);
    private static final VoxelShape SOUTH = box(6, 6, 0, 10, 10, 10);
    private static final VoxelShape WEST = box(6, 6, 6, 16, 10, 10);
    private static final VoxelShape EAST = box(0, 6, 6, 10, 10, 10);

    public BlockMarkerPath(BlockBehaviour.Properties properties) {
        super(properties.strength(0.0F).sound(SoundType.WOOD).noCollision());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BlockMarkerPath> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PathMarkerBlockEntity(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            PathSavedData.get(serverLevel).addMarker(pos);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
        Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            if (level instanceof ServerLevel serverLevel) PathSavedData.get(serverLevel).removeMarker(pos);
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && PathSavedData.get(serverLevel).reverse(pos)) {
            return InteractionResult.SUCCESS;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel) PathSavedData.get(serverLevel).removeMarker(pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
