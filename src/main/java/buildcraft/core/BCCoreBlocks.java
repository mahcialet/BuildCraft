package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.core.block.BlockDecoration;
import buildcraft.core.block.BlockMarkerPath;
import buildcraft.core.block.BlockMarkerVolume;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block registrations migrated from the BuildCraft core module. */
public final class BCCoreBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraft.MOD_ID);

    public static final DeferredBlock<BlockDecoration> DECORATED =
        BLOCKS.registerBlock("decorated", BlockDecoration::new);
    public static final DeferredBlock<BlockMarkerPath> MARKER_PATH =
        BLOCKS.registerBlock("marker_path", BlockMarkerPath::new);
    public static final DeferredBlock<BlockMarkerVolume> MARKER_VOLUME =
        BLOCKS.registerBlock("marker_volume", BlockMarkerVolume::new);

    private BCCoreBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
