package buildcraft.factory.block;

import buildcraft.api.tools.IWrenchable;
import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.block.entity.FloodGateBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public final class FloodGateBlock extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<FloodGateBlock> CODEC = simpleCodec(FloodGateBlock::new);
    public static final BooleanProperty DOWN = BooleanProperty.create("connected_down");
    public static final BooleanProperty NORTH = BooleanProperty.create("connected_north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("connected_south");
    public static final BooleanProperty WEST = BooleanProperty.create("connected_west");
    public static final BooleanProperty EAST = BooleanProperty.create("connected_east");

    public FloodGateBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(2.0F).sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(DOWN, true).setValue(NORTH, true)
            .setValue(SOUTH, true).setValue(WEST, true).setValue(EAST, true));
    }

    @Override protected MapCodec<? extends FloodGateBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,
        BlockState> builder) {
        builder.add(DOWN, NORTH, SOUTH, WEST, EAST);
    }

    public static @Nullable BooleanProperty property(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> null;
        };
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloodGateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BCFactoryBlockEntities.FLOOD_GATE.get(), FloodGateBlockEntity::tick);
    }

    @Override
    public InteractionResult onWrenched(UseOnContext context) {
        if (context.getClickedFace() == Direction.UP) return InteractionResult.FAIL;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof FloodGateBlockEntity gate)) {
            return InteractionResult.FAIL;
        }
        return gate.toggleSide(context.getClickedFace()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
