package buildcraft.builders.block;

import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.block.entity.QuarryBlockEntity;
import buildcraft.api.tools.IWrenchable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import buildcraft.core.AdvancementUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class QuarryBlock extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<QuarryBlock> CODEC = simpleCodec(QuarryBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public QuarryBlock(Properties properties) {
        super(properties.strength(10.0F, 10.0F).sound(SoundType.ANVIL));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            AdvancementUtil.award(player, "buildcraftbuilders:shaping_the_world");
        }
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new QuarryBlockEntity(pos, state); }
    @Override public InteractionResult onWrenched(UseOnContext context) {
        if (!context.getLevel().isClientSide()
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof QuarryBlockEntity quarry) {
            quarry.restart();
        }
        return InteractionResult.SUCCESS;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BCBuildersBlockEntities.QUARRY.get(), QuarryBlockEntity::tick);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof QuarryBlockEntity quarry) quarry.destroy();
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
