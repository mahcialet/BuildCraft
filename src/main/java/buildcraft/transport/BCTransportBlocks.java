package buildcraft.transport;

import buildcraft.transport.block.PipeHolderBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCTransportBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCTransport.MOD_ID);
    public static final DeferredBlock<PipeHolderBlock> PIPE_HOLDER =
        BLOCKS.registerBlock("pipe_holder", PipeHolderBlock::new);

    private BCTransportBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
