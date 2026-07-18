package buildcraft.energy.block;

import buildcraft.api.tools.IWrenchable;
import buildcraft.energy.BCEnergyBlockEntities;
import buildcraft.energy.block.entity.DynamoMjBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
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

public final class BlockDynamoMj extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<BlockDynamoMj> CODEC = simpleCodec(BlockDynamoMj::new);
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    public BlockDynamoMj(BlockBehaviour.Properties properties) {
        super(properties.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BlockDynamoMj> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
        ItemStack stack) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DynamoMjBlockEntity dynamo) {
            dynamo.rotateIfInvalid();
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DynamoMjBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            buildcraft.energy.menu.EngineMenu.open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
        @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DynamoMjBlockEntity dynamo) {
            dynamo.rotateIfInvalid();
        }
    }

    @Override
    public InteractionResult onWrenched(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        return context.getLevel().getBlockEntity(context.getClickedPos()) instanceof DynamoMjBlockEntity dynamo
            && dynamo.rotateToNextReceiver() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        return createTickerHelper(type, BCEnergyBlockEntities.MJ_DYNAMO.get(), DynamoMjBlockEntity::tick);
    }
}
