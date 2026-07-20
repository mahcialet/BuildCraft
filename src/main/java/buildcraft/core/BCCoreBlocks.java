package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.core.block.BlockDecoration;
import buildcraft.core.block.BlockMarkerPath;
import buildcraft.core.block.BlockMarkerVolume;
import buildcraft.core.block.BlockSpring;
import buildcraft.core.block.BlockEngine;
import buildcraft.core.block.PowerTesterBlock;
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
    public static final DeferredBlock<BlockSpring> SPRING =
        BLOCKS.registerBlock("spring", BlockSpring::new);
    public static final DeferredBlock<BlockEngine> ENGINE =
            BLOCKS.registerBlock("engine", BlockEngine::new);
    public static final DeferredBlock<PowerTesterBlock> POWER_TESTER =
            BLOCKS.registerBlock("power_tester", PowerTesterBlock::new);

    private BCCoreBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
