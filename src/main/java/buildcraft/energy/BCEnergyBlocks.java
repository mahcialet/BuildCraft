package buildcraft.energy;

import buildcraft.energy.block.BlockDynamoMj;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCEnergyBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCEnergy.MOD_ID);
    public static final DeferredBlock<BlockDynamoMj> MJ_DYNAMO =
        BLOCKS.registerBlock("mj_dynamo", BlockDynamoMj::new);

    private BCEnergyBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
