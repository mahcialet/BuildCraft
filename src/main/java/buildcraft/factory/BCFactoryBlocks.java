package buildcraft.factory;

import buildcraft.factory.block.TankBlock;
import buildcraft.factory.block.FloodGateBlock;
import buildcraft.factory.block.PumpBlock;
import buildcraft.factory.block.TubeBlock;
import buildcraft.factory.block.MiningWellBlock;
import buildcraft.factory.block.ChuteBlock;
import buildcraft.factory.block.DistillerBlock;
import buildcraft.factory.block.HeatExchangerBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCFactoryBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCFactory.MOD_ID);
    public static final DeferredBlock<TankBlock> TANK = BLOCKS.registerBlock("tank", TankBlock::new);
    public static final DeferredBlock<FloodGateBlock> FLOOD_GATE =
        BLOCKS.registerBlock("flood_gate", FloodGateBlock::new);
    public static final DeferredBlock<PumpBlock> PUMP = BLOCKS.registerBlock("pump", PumpBlock::new);
    public static final DeferredBlock<TubeBlock> TUBE = BLOCKS.registerBlock("tube", TubeBlock::new);
    public static final DeferredBlock<MiningWellBlock> MINING_WELL =
            BLOCKS.registerBlock("mining_well", MiningWellBlock::new);
    public static final DeferredBlock<ChuteBlock> CHUTE = BLOCKS.registerBlock("chute", ChuteBlock::new);
    public static final DeferredBlock<DistillerBlock> DISTILLER =
            BLOCKS.registerBlock("distiller", DistillerBlock::new);
    public static final DeferredBlock<HeatExchangerBlock> HEAT_EXCHANGER =
            BLOCKS.registerBlock("heat_exchange", HeatExchangerBlock::new);

    private BCFactoryBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
