package buildcraft.transport.block;

import buildcraft.transport.BCTransportBlockEntities;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.block.entity.FilteredBufferBlockEntity;
import buildcraft.transport.menu.FilteredBufferMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class FilteredBufferBlock extends BaseEntityBlock {
    public static final MapCodec<FilteredBufferBlock> CODEC = simpleCodec(FilteredBufferBlock::new);

    public FilteredBufferBlock(Properties properties) {
        super(properties.strength(3.0F));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            FilteredBufferMenu.open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilteredBufferBlockEntity(pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (level.getBlockEntity(pos) instanceof FilteredBufferBlockEntity buffer) {
            for (int slot = 0; slot < FilteredBufferBlockEntity.SLOTS; slot++) {
                long amount = buffer.inventory().getAmountAsLong(slot);
                if (amount > 0) net.minecraft.world.Containers.dropItemStack(level,
                        pos.getX(), pos.getY(), pos.getZ(),
                        buffer.inventory().getResource(slot).toStack((int) amount));
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
}
