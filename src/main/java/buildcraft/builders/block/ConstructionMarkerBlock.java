package buildcraft.builders.block;

import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.block.entity.ConstructionMarkerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ConstructionMarkerBlock extends BaseEntityBlock {
    public static final MapCodec<ConstructionMarkerBlock> CODEC = simpleCodec(ConstructionMarkerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final VoxelShape UP = Block.box(5, 0, 5, 11, 12, 11);
    private static final VoxelShape DOWN = Block.box(5, 4, 5, 11, 16, 11);
    private static final VoxelShape NORTH = Block.box(5, 5, 4, 11, 11, 16);
    private static final VoxelShape SOUTH = Block.box(5, 5, 0, 11, 11, 12);
    private static final VoxelShape WEST = Block.box(4, 5, 5, 16, 11, 11);
    private static final VoxelShape EAST = Block.box(0, 5, 5, 12, 11, 11);
    public ConstructionMarkerBlock(Properties properties) {
        super(properties.strength(0.0F).noCollision().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ConstructionMarkerBlockEntity(pos, state); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return canSupportCenter(level, pos.relative(facing.getOpposite()), facing);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbour,
            net.minecraft.util.RandomSource random) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ConstructionMarkerBlockEntity marker
                && buildcraft.builders.item.SnapshotItem.hasSnapshot(stack)) {
            if (!level.isClientSide() && marker.setBlueprint(stack) && !player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ConstructionMarkerBlockEntity marker)
                || marker.blueprint().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack stack = marker.removeBlueprint();
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof ConstructionMarkerBlockEntity marker && !marker.blueprint().isEmpty())
            popResource(level, pos, marker.blueprint());
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN; case UP -> UP; case NORTH -> NORTH;
            case SOUTH -> SOUTH; case WEST -> WEST; case EAST -> EAST;
        };
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
}
