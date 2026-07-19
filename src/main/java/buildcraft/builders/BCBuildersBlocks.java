package buildcraft.builders;

import buildcraft.builders.block.FillerBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCBuildersBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCBuilders.MOD_ID);
    public static final DeferredBlock<FillerBlock> FILLER = BLOCKS.registerBlock("filler", FillerBlock::new);

    private BCBuildersBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
