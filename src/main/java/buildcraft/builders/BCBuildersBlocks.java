package buildcraft.builders;

import buildcraft.builders.block.FillerBlock;
import buildcraft.builders.block.ArchitectTableBlock;
import buildcraft.builders.block.BuilderBlock;
import buildcraft.builders.block.ReplacerBlock;
import buildcraft.builders.block.FrameBlock;
import buildcraft.builders.block.QuarryBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCBuildersBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCBuilders.MOD_ID);
    public static final DeferredBlock<FillerBlock> FILLER = BLOCKS.registerBlock("filler", FillerBlock::new);
    public static final DeferredBlock<ArchitectTableBlock> ARCHITECT_TABLE =
            BLOCKS.registerBlock("architect_table", ArchitectTableBlock::new);
    public static final DeferredBlock<BuilderBlock> BUILDER = BLOCKS.registerBlock("builder", BuilderBlock::new);
    public static final DeferredBlock<ReplacerBlock> REPLACER = BLOCKS.registerBlock("replacer", ReplacerBlock::new);
    public static final DeferredBlock<QuarryBlock> QUARRY = BLOCKS.registerBlock("quarry", QuarryBlock::new);
    public static final DeferredBlock<FrameBlock> FRAME = BLOCKS.registerBlock("frame", FrameBlock::new);

    private BCBuildersBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
