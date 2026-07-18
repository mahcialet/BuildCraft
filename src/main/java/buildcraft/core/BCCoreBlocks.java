package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.core.block.BlockDecoration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block registrations migrated from the BuildCraft core module. */
public final class BCCoreBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraft.MOD_ID);

    public static final DeferredBlock<BlockDecoration> DECORATED =
        BLOCKS.registerBlock("decorated", BlockDecoration::new);

    private BCCoreBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
