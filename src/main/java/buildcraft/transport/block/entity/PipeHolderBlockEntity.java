package buildcraft.transport.block.entity;

import buildcraft.transport.BCTransportBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent extension point for pipe flow, pluggable, and wire state. */
public final class PipeHolderBlockEntity extends BlockEntity {
    public PipeHolderBlockEntity(BlockPos pos, BlockState state) {
        super(BCTransportBlockEntities.PIPE_HOLDER.get(), pos, state);
    }
}
