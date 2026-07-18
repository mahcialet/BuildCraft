package buildcraft.transport.block;

import buildcraft.transport.BCTransportBlockEntities;
import buildcraft.transport.PipeType;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import buildcraft.transport.item.PipeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class PipeHolderBlock extends BaseEntityBlock {
    public static final MapCodec<PipeHolderBlock> CODEC = simpleCodec(PipeHolderBlock::new);
    public static final EnumProperty<PipeType> TYPE = EnumProperty.create("type", PipeType.class);
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    private static final BooleanProperty[] CONNECTIONS = { DOWN, UP, NORTH, SOUTH, WEST, EAST };
    private static final VoxelShape CENTER = Block.box(4, 4, 4, 12, 12, 12);
    private static final VoxelShape[] ARMS = {
        Block.box(4, 0, 4, 12, 4, 12), Block.box(4, 12, 4, 12, 16, 12),
        Block.box(4, 4, 0, 12, 12, 4), Block.box(4, 4, 12, 12, 12, 16),
        Block.box(0, 4, 4, 4, 12, 12), Block.box(12, 4, 4, 16, 12, 12)
    };

    public PipeHolderBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(0.25F).sound(SoundType.METAL).noOcclusion());
        BlockState state = stateDefinition.any().setValue(TYPE, PipeType.STRUCTURE);
        for (BooleanProperty connection : CONNECTIONS) state = state.setValue(connection, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends PipeHolderBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        PipeType type = context.getItemInHand().getItem() instanceof PipeItem item
            ? item.pipeType() : PipeType.STRUCTURE;
        BlockState state = defaultBlockState().setValue(TYPE, type);
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), connects(type,
                context.getLevel().getBlockState(context.getClickedPos().relative(direction))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
        Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(property(direction), connects(state.getValue(TYPE), neighbourState));
    }

    private static boolean connects(PipeType type, BlockState neighbour) {
        return neighbour.getBlock() instanceof PipeHolderBlock
            && type.connectsTo(neighbour.getValue(TYPE));
    }

    private static BooleanProperty property(Direction direction) {
        return CONNECTIONS[direction.ordinal()];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        for (Direction direction : Direction.values()) {
            if (state.getValue(property(direction))) shape = Shapes.or(shape, ARMS[direction.ordinal()]);
        }
        return shape;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeHolderBlockEntity(pos, state);
    }
}
