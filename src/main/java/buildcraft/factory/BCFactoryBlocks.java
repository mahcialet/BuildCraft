package buildcraft.factory;

import buildcraft.factory.block.TankBlock;
import buildcraft.factory.block.FloodGateBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCFactoryBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCFactory.MOD_ID);
    public static final DeferredBlock<TankBlock> TANK = BLOCKS.registerBlock("tank", TankBlock::new);
    public static final DeferredBlock<FloodGateBlock> FLOOD_GATE =
        BLOCKS.registerBlock("flood_gate", FloodGateBlock::new);

    private BCFactoryBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
