package buildcraft.core.block;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.tools.IWrenchable;
import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.block.entity.RedstoneEngineBlockEntity;
import buildcraft.core.block.entity.CreativeEngineBlockEntity;
import buildcraft.core.block.entity.EngineBlockEntity;
import buildcraft.energy.BCEnergyBlockEntities;
import buildcraft.energy.block.entity.StirlingEngineBlockEntity;
import buildcraft.energy.block.entity.CombustionEngineBlockEntity;
import buildcraft.energy.block.entity.RfEngineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
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

/** Shared block for the engine variants registered by Core and Energy. */
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
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EngineBlockEntity engine) {
            engine.rotateIfInvalid();
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (state.getValue(ENGINE_TYPE)) {
            case WOOD -> new RedstoneEngineBlockEntity(pos, state);
            case STONE -> new StirlingEngineBlockEntity(pos, state);
            case IRON -> new CombustionEngineBlockEntity(pos, state);
            case RF -> new RfEngineBlockEntity(pos, state);
            case CREATIVE -> new CreativeEngineBlockEntity(pos, state);
            default -> null;
        };
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (type == BCCoreBlockEntities.ENGINE_REDSTONE.get()) {
            return createTickerHelper(type, BCCoreBlockEntities.ENGINE_REDSTONE.get(), RedstoneEngineBlockEntity::tick);
        }
        if (type == BCEnergyBlockEntities.ENGINE_STIRLING.get()) {
            return createTickerHelper(type, BCEnergyBlockEntities.ENGINE_STIRLING.get(), StirlingEngineBlockEntity::tick);
        }
        if (type == BCEnergyBlockEntities.ENGINE_COMBUSTION.get()) {
            return createTickerHelper(type, BCEnergyBlockEntities.ENGINE_COMBUSTION.get(), CombustionEngineBlockEntity::tick);
        }
        if (type == BCEnergyBlockEntities.ENGINE_RF.get()) {
            return createTickerHelper(type, BCEnergyBlockEntities.ENGINE_RF.get(), RfEngineBlockEntity::tick);
        }
        return createTickerHelper(type, BCCoreBlockEntities.ENGINE_CREATIVE.get(), CreativeEngineBlockEntity::tick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
        @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EngineBlockEntity engine) {
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
        if (blockEntity instanceof CreativeEngineBlockEntity creative) {
            int index = creative.cycleOutput();
            if (context.getPlayer() != null) {
                context.getPlayer().sendOverlayMessage(Component.translatable(
                    "chat.buildcraftcore.engine.output", CreativeEngineBlockEntity.OUTPUTS[index]
                ));
            }
            return InteractionResult.SUCCESS;
        }
        return blockEntity instanceof EngineBlockEntity engine && engine.rotateToNextReceiver()
            ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hitResult) {
        EnumEngineType type = state.getValue(ENGINE_TYPE);
        if (type != EnumEngineType.STONE && type != EnumEngineType.IRON && type != EnumEngineType.RF) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            buildcraft.energy.menu.EngineMenu.open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (state.getValue(ENGINE_TYPE) == EnumEngineType.IRON
            && level.getBlockEntity(pos) instanceof buildcraft.energy.block.entity.CombustionEngineBlockEntity engine
            && net.neoforged.neoforge.transfer.fluid.FluidUtil.interactWithFluidHandler(
                player, hand, pos, engine.tanks()
            )) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
