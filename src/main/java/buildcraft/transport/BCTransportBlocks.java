package buildcraft.transport;

import buildcraft.transport.block.PipeHolderBlock;
import buildcraft.transport.block.FilteredBufferBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCTransportBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCTransport.MOD_ID);
    public static final DeferredBlock<PipeHolderBlock> PIPE_HOLDER =
        BLOCKS.registerBlock("pipe_holder", PipeHolderBlock::new);
    public static final DeferredBlock<FilteredBufferBlock> FILTERED_BUFFER =
        BLOCKS.registerBlock("filtered_buffer", FilteredBufferBlock::new);

    private BCTransportBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
