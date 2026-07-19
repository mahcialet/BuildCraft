package buildcraft.robotics;

import buildcraft.robotics.block.RequesterBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCRoboticsBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCRobotics.MOD_ID);
    public static final DeferredBlock<RequesterBlock> REQUESTER =
            BLOCKS.registerBlock("requester", RequesterBlock::new);
    private BCRoboticsBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
