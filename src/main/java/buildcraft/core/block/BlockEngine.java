package buildcraft.core.block;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.tools.IWrenchable;
import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.block.entity.RedstoneEngineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/** Shared engine block, initially backed by the core redstone-engine entity. */
public final class BlockEngine extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<BlockEngine> CODEC = simpleCodec(BlockEngine::new);
    public static final EnumProperty<EnumEngineType> ENGINE_TYPE =
        EnumProperty.create("type", EnumEngineType.class);
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    public BlockEngine(BlockBehaviour.Properties properties) {
        super(properties.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any()
            .setValue(ENGINE_TYPE, EnumEngineType.WOOD)
            .setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BlockEngine> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENGINE_TYPE, FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ENGINE_TYPE, EnumEngineType.WOOD);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
        ItemStack stack) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            engine.rotateIfInvalid();
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(ENGINE_TYPE) == EnumEngineType.WOOD
            ? new RedstoneEngineBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        return createTickerHelper(type, BCCoreBlockEntities.ENGINE_REDSTONE.get(), RedstoneEngineBlockEntity::tick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
        @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            engine.rotateIfInvalid();
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.invalidateCapabilities(pos);
    }

    @Override
    public InteractionResult onWrenched(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        return blockEntity instanceof RedstoneEngineBlockEntity engine && engine.rotateToNextReceiver()
            ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
