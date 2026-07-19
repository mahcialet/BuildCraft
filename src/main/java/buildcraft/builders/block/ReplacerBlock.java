package buildcraft.builders.block;

import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.block.entity.ReplacerBlockEntity;
import buildcraft.builders.menu.ReplacerMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class ReplacerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<ReplacerBlock> CODEC = simpleCodec(ReplacerBlock::new);
    public ReplacerBlock(Properties properties) {
        super(properties.strength(3.0F)); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                     Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer server) ReplacerMenu.open(server, pos);
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer server) ReplacerMenu.open(server, pos);
        return InteractionResult.SUCCESS;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ReplacerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BCBuildersBlockEntities.REPLACER.get(), ReplacerBlockEntity::tick);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof ReplacerBlockEntity replacer) for (int slot = 0; slot < 3; slot++) {
            long amount = replacer.inventory().getAmountAsLong(slot);
            if (amount > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    replacer.inventory().getResource(slot).toStack((int) amount));
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
